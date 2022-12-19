convert icon512.png -resize 144x144 app/src/main/res/drawable-xxhdpi/icon.png
convert icon512.png -resize 96x96 app/src/main/res/drawable-xhdpi/icon.png
convert icon512.png -resize 72x72 app/src/main/res/drawable-hdpi/icon.png
convert icon512.png -resize 48x48 app/src/main/res/drawable-mdpi/icon.png
convert icon512.png -resize 36x36 app/src/main/res/drawable-ldpi/icon.png

for x in cameraswitch fast_rewind ; do
    convert $x.png -resize 96x96 app/src/main/res/drawable-xxhdpi/$x.png
    convert $x.png -resize 72x72 app/src/main/res/drawable-xhdpi/$x.png
    convert $x.png -resize 48x48 app/src/main/res/drawable-hdpi/$x.png
    convert $x.png -resize 36x36 app/src/main/res/drawable-mdpi/$x.png
    convert $x.png -resize 24x24 app/src/main/res/drawable-ldpi/$x.png
done
    
    
#for x in zero ; do
#   convert res/drawable-xhdpi/$x.png -resize 75% res/drawable-hdpi/$x.png
#   convert res/drawable-xhdpi/$x.png -resize 50% res/drawable-mdpi/$x.png
#   convert res/drawable-xhdpi/$x.png -resize 37.5% res/drawable-ldpi/$x.png
#done
