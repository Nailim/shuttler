import pygame

pygame.init()

w = 640
h = 480
size=(w,h)
screen = pygame.display.set_mode(size)

while (True) :
	img = pygame.image.load("shot.jpg")
	screen.blit(img,(0,0))
	pygame.display.flip()
	print "***"
