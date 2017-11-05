import Tkinter as tk
from PIL import Image, ImageTk

root = tk.Tk()
root.title('background image')

imageFile = "shot.jpg"
image1 = ImageTk.PhotoImage(Image.open(imageFile))

# get the image size
w = image1.width()
h = image1.height()

# position coordinates of root 'upper left corner'
x = 0
y = 0

# make the root window the size of the image
root.geometry("%dx%d+%d+%d" % (w, h, x, y))

# root has no image argument, so use a label as a panel
panel1 = tk.Label(root, image=image1)
panel1.pack(side='top', fill='both', expand='yes')

# save the panel's image from 'garbage collection'
panel1.image = image1

# start the event loop
root.mainloop()
