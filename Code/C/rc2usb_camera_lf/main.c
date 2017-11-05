#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <stdlib.h>

// USART definitions
#define BAUD 57600
#define FOSC 20000000			// clock Speed
#define MYUBRR FOSC/16/BAUD-1

// USART init
void USART_Init( unsigned int ubrr) {
	// set baud rate
	UBRR0H = (unsigned char)(ubrr>>8);
	UBRR0L = (unsigned char)ubrr;

	// enable receiver and transmitter
	UCSR0B = (1<<RXEN0)|(1<<TXEN0);
	
	// set frame format: 8data, 2stop bit
	UCSR0C = (1<<USBS0)|(3<<UCSZ00);
}

// USART transmit
void USART_Transmit( unsigned char data ) {
	// wait for empty transmit buffer
	while ( !( UCSR0A & (1<<UDRE0)) );
	
	// put data into buffer, sends the data
	UDR0 = data;
}


int main(void) {
	// set PD2 as output - LED
	DDRD |= 0x04;
	PORTD &= ~0x04;		// LED (PD2) off
	
	DDRC = 0xFF;	// PORTC as output
	PORTC = 0x00;
	
	// init USART
	USART_Init(MYUBRR);

	
	// do nothing and repeat - interrupts do all the work
	// PC0-PC5 on the right side, PD4 on left side. 7 bits altogether. Also PD3, so 8 bits.
	for(;;){
		/*
		PORTD = PORTD | 0x04;				
		PORTC = 0xFF;
		_delay_ms(1250);
		PORTD = PORTD & ~(0x04);		
		PORTC = 0x00;
		_delay_ms(2500);
		*/
		PORTD = PORTD | 0x04;	// LED
		PORTC = 0x07;	// Prva kamera
		_delay_ms(1500);
		PORTC = 0x00;	// Spustimo prvo kamero
		PORTD = PORTD & ~(0x04);	// LED
		_delay_ms(1250);		
		PORTC = 0x38;	// Druga kamera
		_delay_ms(1500);
		PORTC = 0x00;	// Spustimo drugo kamero
		_delay_ms(1500);

	}
}
