#!/usr/bin/env python3
"""
split_key_clicks.py - Analyzes recorded key click WAV files and splits them into
separate Key Down (press) and Key Up (release) WAV audio files based on acoustic
content (envelope energy, transient peaks, zero crossings, and silence gap analysis).
"""

import os
import sys
import wave
import numpy as np

def analyze_and_split(filepath, output_dir):
    filename = os.path.basename(filepath)
    base_name, ext = os.path.splitext(filename)
    
    with wave.open(filepath, 'rb') as w:
        nchannels = w.getnchannels()
        sampwidth = w.getsampwidth()
        framerate = w.getframerate()
        nframes = w.getnframes()
        comptype = w.getcomptype()
        compname = w.getcompname()
        raw_data = w.readframes(nframes)

    # Convert audio samples to float normalized array (-1.0 to 1.0)
    if sampwidth == 2:
        samples = np.frombuffer(raw_data, dtype=np.int16).astype(np.float32) / 32768.0
    elif sampwidth == 1:
        samples = (np.frombuffer(raw_data, dtype=np.uint8).astype(np.float32) - 128.0) / 128.0
    elif sampwidth == 3:
        raw_bytes = np.frombuffer(raw_data, dtype=np.uint8)
        a24 = np.zeros(nframes * nchannels, dtype=np.int32)
        a24.view(np.uint8)[1:4] = raw_bytes
        samples = (a24 >> 8).astype(np.float32) / 8388608.0
    else:
        raise ValueError(f"Unsupported sample width: {sampwidth}")

    duration_ms = (len(samples) / framerate) * 1000.0

    # 1. Compute 1ms RMS energy and peak amplitude envelope
    win_len = int(framerate * 0.001)
    hop_len = int(framerate * 0.0005) # 0.5ms step
    
    num_wins = (len(samples) - win_len) // hop_len + 1
    times = np.array([(i * hop_len + win_len / 2.0) / framerate * 1000.0 for i in range(num_wins)])
    peaks = np.array([np.max(np.abs(samples[i*hop_len : i*hop_len + win_len])) for i in range(num_wins)])
    rms = np.array([np.sqrt(np.mean(samples[i*hop_len : i*hop_len + win_len]**2)) for i in range(num_wins)])

    # 2. Find Key Down peak (primary transient in first 50ms)
    kd_search_end = min(len(peaks), int(50.0 / 0.5))
    kd_peak_idx = np.argmax(peaks[:kd_search_end])
    kd_peak_time = times[kd_peak_idx]

    # 3. Determine split boundary based on duration and acoustic profile
    if duration_ms > 80.0:
        # Long clip (recorded mechvibes full keystrokes, ~100-185ms)
        # Search for KeyUp transient in second half (>65ms)
        ku_search_start = int(65.0 / 0.5)
        if ku_search_start < len(peaks):
            ku_peaks = peaks[ku_search_start:]
            ku_peak_rel = np.argmax(ku_peaks)
            ku_peak_idx = ku_search_start + ku_peak_rel
            ku_peak_time = times[ku_peak_idx]
            ku_peak_val = peaks[ku_peak_idx]
        else:
            ku_peak_idx = len(peaks) - 1
            ku_peak_time = duration_ms
            ku_peak_val = 0.0

        # Search for minimum RMS energy gap between keydown decay (60ms) and keyup onset
        search_s = int(55.0 / 0.5)
        search_e = min(len(rms) - 1, int(max(70.0, ku_peak_time - 5.0) / 0.5))
        
        gap_rms = rms[search_s : search_e + 1]
        split_idx = search_s + np.argmin(gap_rms)
        split_time = times[split_idx]
    else:
        # Short clip (synthesized / short click samples, ~30-45ms)
        search_s = int(12.0 / 0.5)
        search_e = min(len(rms) - 1, int(25.0 / 0.5))
        gap_rms = rms[search_s : search_e + 1]
        split_idx = search_s + np.argmin(gap_rms)
        split_time = times[split_idx]
        ku_peak_time = times[search_s + np.argmax(peaks[search_s:])]

    split_sample = int(split_time / 1000.0 * framerate)
    split_sample = max(0, min(len(samples) - 1, split_sample))

    # 4. Snap split sample to nearest Zero Crossing in a ±2ms window to prevent clicking
    search_window = int(framerate * 0.002) # ±2ms
    s_start = max(0, split_sample - search_window)
    s_end = min(len(samples) - 1, split_sample + search_window)
    
    zero_crossings = []
    for s in range(s_start, s_end):
        if samples[s] * samples[s+1] <= 0:
            zero_crossings.append(s)
            
    if zero_crossings:
        split_sample = min(zero_crossings, key=lambda z: abs(z - split_sample))
        split_time = (split_sample / framerate) * 1000.0

    # Extract Key Down audio segment (0 to split_sample)
    keydown_samples = samples[:split_sample].copy()
    
    # Extract Key Up audio segment (split_sample to end)
    keyup_samples = samples[split_sample:].copy()

    # Trim leading silence from keyup audio so keyup plays immediately on key release
    keyup_max = np.max(np.abs(keyup_samples)) if len(keyup_samples) > 0 else 0
    silence_thresh = max(0.0015, keyup_max * 0.04)
    
    start_trim = 0
    for s in range(len(keyup_samples)):
        if abs(keyup_samples[s]) > silence_thresh:
            # Leave 1.5ms pre-padding before transient
            start_trim = max(0, s - int(framerate * 0.0015))
            break
            
    keyup_trimmed = keyup_samples[start_trim:].copy()

    # Apply 1ms micro fade-out to keydown end and 1ms micro fade-in to keyup start
    fade_len = int(framerate * 0.001)
    if len(keydown_samples) > fade_len:
        fade_out = np.linspace(1.0, 0.0, fade_len, dtype=np.float32)
        keydown_samples[-fade_len:] *= fade_out

    if len(keyup_trimmed) > fade_len:
        fade_in = np.linspace(0.0, 1.0, fade_len, dtype=np.float32)
        keyup_trimmed[:fade_len] *= fade_in

    # Convert back to 16-bit PCM bytes
    def to_pcm16(float_arr):
        clipped = np.clip(float_arr, -1.0, 1.0)
        return (clipped * 32767.0).astype(np.int16).tobytes()

    os.makedirs(output_dir, exist_ok=True)

    down_path = os.path.join(output_dir, f"{base_name}_down.wav")
    up_path = os.path.join(output_dir, f"{base_name}_up.wav")

    with wave.open(down_path, 'wb') as w:
        w.setparams((nchannels, sampwidth, framerate, len(keydown_samples), comptype, compname))
        w.writeframes(to_pcm16(keydown_samples))

    with wave.open(up_path, 'wb') as w:
        w.setparams((nchannels, sampwidth, framerate, len(keyup_trimmed), comptype, compname))
        w.writeframes(to_pcm16(keyup_trimmed))

    print(f"[{filename:28s}] Split at {split_time:6.2f}ms (sample {split_sample:5d}/{len(samples)})")
    print(f"  └─ Key Down: {len(keydown_samples)/framerate*1000:5.1f}ms -> {os.path.basename(down_path)}")
    print(f"  └─ Key Up  : {len(keyup_trimmed)/framerate*1000:5.1f}ms -> {os.path.basename(up_path)}")

    return {
        'filename': filename,
        'duration_ms': duration_ms,
        'split_time_ms': split_time,
        'split_sample': split_sample,
        'down_path': down_path,
        'up_path': up_path,
        'keydown_dur_ms': len(keydown_samples)/framerate*1000.0,
        'keyup_dur_ms': len(keyup_trimmed)/framerate*1000.0,
    }

def main():
    src_dir = 'app/src/main/assets/audio'
    out_dir = 'app/src/main/assets/audio_split'

    if len(sys.argv) > 1:
        src_dir = sys.argv[1]
    if len(sys.argv) > 2:
        out_dir = sys.argv[2]

    print(f"Analyzing key clicks from '{src_dir}'...")
    print(f"Outputting split audio files to '{out_dir}'...\n")

    files = sorted([f for f in os.listdir(src_dir) if f.endswith('.wav')])
    results = []
    for f in files:
        fpath = os.path.join(src_dir, f)
        res = analyze_and_split(fpath, out_dir)
        results.append(res)

    print(f"\nSuccessfully processed {len(results)} audio files into '{out_dir}'!")

if __name__ == '__main__':
    main()
