### inside the layout editor tab
move the layout selector outside of the box above it. 
the selection list should come from the list of all layouts.
default layouts should be static. they're only purpose is to be copied to a user at a total versionin case it doesn't exist or in case it is requested to be set to back to default.
make sure that we can set the key label to and included SVG file or have an image provided by the user. make sure the image can be browsed for. have a preview of The Graphic in questionin the drop down.
the parameters for a key's parameters should be selected from a drop down or entered depending on the action.
we need to make sure that a space character is a valid value for a key to output. it should not be overridden by the label character
highlight the save button if changes have been made.
resize the key editor window to avoid the keyboard. it should probe to see if the keyboard is enabled anytime any widget is selected.
the key editor should scroll if the editor area is larger than the available.
move the save line to the bottom of the layout editor box.
remove the text from that line,put the redo / undo buttons on the left and the save button on the right.
make sure to highlight the save button if changes were made
for the import/export section,make the note say that they can edit the files live in a saf enabled editor. add a note saying they can have direct access to those files using the files application. had a launcher button to launch the files application to that path.
each key should have an auto repeat Boolean as one of the long press behaviors
make sure spacing between Keys is editable. the preview keyboard should not respond to anything except for key presses to launch the key editor or spacing editor

### themes editor tab
make the equivalent layout changes to the themes tab similar to the layout tab.

### keyboard functionality
if a key has engaged the long press action, do not interrupt due to a drag until released

### default layout updates
remove the hist text from the clipboard button.

### split keyboard rendering
when splitting the keyboard,make sure the keys are in the equivalent spot as if the keyboard was not split. as if you took a knife and to just cut along the defined line between keys.

## git
after a successful build, run xdg-open on the debug APK file. 
do a test build between every line item changed.
between line items, get confirmation to move to the next item. 
do not make changes to any other section of the code unless we have defined a reason.
bump the bug fix version and commit that change for every successful completion of a task
