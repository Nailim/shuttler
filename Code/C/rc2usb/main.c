#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>
#include <stdlib.h>

// USART definitions
#define BAUD 57600
#define FOSC 20000000			// clock Speed
#define MYUBRR FOSC/16/BAUD-1

// global variables
volatile unsigned char port_c_last = 0x00;		// pin change interrupts PC0-5
volatile unsigned char port_c_change = 0x00;	// pin change interrupts PC0-5
volatile unsigned char port_d_last = 0x00;		// pin change interrupts PD3-4
volatile unsigned char port_d_change = 0x00;	// pin change interrupts PD3-4

volatile unsigned short timer_temp = 0x0000;
volatile unsigned short timer_temp_result[8] = {0x0EA6, 0x0EA6, 0x0EA6, 0x0EA6, 0x0EA6, 0x0EA6, 0x0EA6, 0x0EA6};	// 0x0EA6=3750 - after conversion the output is 0
volatile unsigned short timer_temp_storage[8] = {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};	// storage like any other

volatile unsigned char counter_low = 0x00;
volatile unsigned char counter_high = 0x00;
volatile unsigned short temp_storage = 0x0000;

volatile float data_out = 0.0;					// output data in the right format for flightgear to parse
char buffer_out[10];							// flightgear likes strings
unsigned char buffer_out_pos;					// char by char eaquals string

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

// pin change interrupt alias
ISR(PCINT2_vect, ISR_ALIASOF(PCINT1_vect));

// pin change interrupt
ISR(PCINT1_vect) {
	// get the timer, get the timer NOW!
	timer_temp = TCNT1;	
	
	// check which pin triggerd the interrupt
	port_c_change = (PINC & PCMSK1) ^ port_c_last;
	port_d_change = (PIND & PCMSK2) ^ port_d_last;
	port_c_last = (PINC & PCMSK1);
	port_d_last = (PIND & PCMSK2);
	
	// find and service the apropriate pins
	if (port_c_change & 0x20) {			// channel 1 (PC5 - PCINT 13 - PCMSK1 bit 5)
		if (port_c_last & 0x20) {		// low to hight
			timer_temp_storage[0] = timer_temp;
		}
		else {							// high to low
			if (timer_temp >= timer_temp_storage[0]) {
				timer_temp_result[0] = timer_temp - timer_temp_storage[0];
			}
			else {
				timer_temp_result[0] = (ICR1 - timer_temp_storage[0]) + timer_temp;
			}
		}
	}
	if (port_c_change & 0x10) {			// channel 2 (PC4 - PCINT 12 - PCMSK1 bit 4)
		if (port_c_last & 0x10) {		// low to hight
			timer_temp_storage[1] = timer_temp;
		}
		else {							// high to low
			if (timer_temp >= timer_temp_storage[1]) {
				timer_temp_result[1] = timer_temp - timer_temp_storage[1];
			}
			else {
				timer_temp_result[1] = (ICR1 - timer_temp_storage[1]) + timer_temp;
			}
		}
	}
	if (port_c_change & 0x08) {			// channel 3 (PC3 - PCINT 11 - PCMSK1 bit 3)
		if (port_c_last & 0x08) {		// low to hight
			timer_temp_storage[2] = timer_temp;
		}
		else {							// high to low			
			if (timer_temp >= timer_temp_storage[2]) {
				timer_temp_result[2] = timer_temp - timer_temp_storage[2];
			}
			else {
				timer_temp_result[2] = (ICR1 - timer_temp_storage[2]) + timer_temp;
			}
		}
	}
	if (port_c_change & 0x04) {			// channel 4 (PC2 - PCINT 10 - PCMSK1 bit 2)
		if (port_c_last & 0x04) {		// low to hight
			timer_temp_storage[3] = timer_temp;
		}
		else {							// high to low			
			if (timer_temp >= timer_temp_storage[3]) {
				timer_temp_result[3] = timer_temp - timer_temp_storage[3];
			}
			else {
				timer_temp_result[3] = (ICR1 - timer_temp_storage[3]) + timer_temp;
			}
		}
	}
	if (port_c_change & 0x02) {			// channel 5 (PC1 - PCINT 9 - PCMSK1 bit 1)
		if (port_c_last & 0x02) {		// low to hight
			timer_temp_storage[4] = timer_temp;
		}
		else {							// high to low			
			if (timer_temp >= timer_temp_storage[4]) {
				timer_temp_result[4] = timer_temp - timer_temp_storage[4];
			}
			else {
				timer_temp_result[4] = (ICR1 - timer_temp_storage[4]) + timer_temp;
			}
		}
	}
	if (port_c_change & 0x01) {			// channel 6 (PC0 - PCINT 8 - PCMSK1 bit 0)
		if (port_c_last & 0x01) {		// low to hight
			timer_temp_storage[5] = timer_temp;
		}
		else {							// high to low
			if (timer_temp >= timer_temp_storage[5]) {
				timer_temp_result[5] = timer_temp - timer_temp_storage[5];
			}
			else {
				timer_temp_result[5] = (ICR1 - timer_temp_storage[5]) + timer_temp;
			}
		}
	}
	if (port_d_change & 0x08) {			// channel 7 (PD3 - PCINT 19 - PCMSK2 bit 3)
		if (port_d_last & 0x08) {		// low to hight
			timer_temp_storage[6] = timer_temp;
		}
		else {							// high to low
			if (timer_temp >= timer_temp_storage[6]) {
				timer_temp_result[6] = timer_temp - timer_temp_storage[6];
			}
			else {
				timer_temp_result[6] = (ICR1 - timer_temp_storage[6]) + timer_temp;
			}
		}
	}
	if (port_d_change & 0x10) {			// channel 8 (PD4 - PCINT 20 - PCMSK2 bit 4)
		if (port_d_last & 0x10) {		// low to hight
			timer_temp_storage[7] = timer_temp;
		}
		else {							// high to low			
			if (timer_temp >= timer_temp_storage[7]) {
				timer_temp_result[7] = timer_temp - timer_temp_storage[7];
			}
			else {
				timer_temp_result[7] = (ICR1 - timer_temp_storage[7]) + timer_temp;
			}
		}
	}	
}

ISR(TIMER1_COMPB_vect) {
	// turn off other interrupts (servo duty cycle is 50Hz, so we get at least every other one while we're not sending)
	cli();	// optional - works without, but why waste cycles reading new servo state if we read it at least once more before we send it
	
	// update the conter, update the counter now
	counter_low++;
	
	// run 25 times per second - transmit and more
	if (counter_low >= 2) {
		// transmmit - the slowest part of them all (one by one, divide and convert)
		temp_storage = timer_temp_result[0];				// get the last known servo state, so it's not overwritten if we got an interrupt in an interrupt
		data_out = (temp_storage / 1250.0) - 3.0 ;			// formula for converting 2500 to 5000 counter cycles in to -1.0 to 1.0 range (flightgear likes)
		dtostrf(data_out, 0, 2, buffer_out);				// flightgear likes strings, converting float to string (2 decimal precisin)
		buffer_out_pos = 0;									// char in string counter
		while (buffer_out[buffer_out_pos] != '\0') {		// send data
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');								// send delimiter
		
		temp_storage = timer_temp_result[1];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[2];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[3];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[4];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[5];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[6];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[7];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		dtostrf(data_out, 0, 2, buffer_out);
		buffer_out_pos = 0;
		while (buffer_out[buffer_out_pos] != '\0') {
			USART_Transmit(buffer_out[buffer_out_pos]);
			buffer_out_pos++;	
		}
		USART_Transmit('\n');
		
		counter_low = 0;
		counter_high++;
		
		// run 1 time per second - set servo position and LED status
		if (counter_high == 25) {
			OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
		}
		else if (counter_high == 50) {
			OCR1A = (2500*1.0);		// servo left position
			PORTD &= ~0x04;			// LED off
		}
		else if (counter_high == 75) {
			OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
		}
		else if (counter_high == 100) {
			OCR1A = (2500*2.0);		// servo right position
			PORTD &= ~0x04;			// LED off
			counter_high = 0;		// restart the cycle
		}
		
	}
	// enable back all interrupts
	sei();	// optional - use if interrupts disabled at the beginning
}

int main(void) {
	// set PD2 as output - LED
	DDRD |= 0x04;
	PORTD &= ~0x04;		// LED (PD2) off
	
	// set PB1 as output - PWM
	DDRB |= 0x02;
	
	// set PC0-5 (PCI1) & PD3-4 (PCI2) as input - PIN CHANGE INTERRUPT
	DDRC &= ~0x3F;
	DDRD &= ~0x19;	// includes the RX to FT230X
	
	// set internal pull up resistors
	PORTC |= 0x3F;
	PORTD |= 0x19;
	
	
	// init USART
	USART_Init(MYUBRR);
	
	// set Timer/Counter1 with PWM
	TCCR1A |= 1<<WGM11;					// set Waveform Generation Mode to 14 (fast PWM - ICR1)
	TCCR1B |= 1<<WGM12 | 1<<WGM13;		// set Waveform Generation Mode to 14 (fast PWM - ICR1)
	//TCCR1A |= 1<<COM1A1 | 1<<COM1A0;	// set inverted mode (starts at 0, jumps to 1 when OCR1A is reached)
	TCCR1A |= 1<<COM1A1;				// set non-inverted mode (starts at 1, jumps to 0 when OCR1A is reached)
	TCCR1B |= 1<<CS11;					// set prescaler to 8 (50Hz duty cycle for servo - ((20000000/8)/50) = 50000)	
	ICR1 = 49999;						// set the Input Capture Register for 50Hz duty cycle (0-49999)
	// set Timer/Counter1 with interrupt
	TIMSK1 |= 1<<OCIE1B;				// set interrupt on CTC
	OCR1B = 49999;						// set Output Compare Register for 50Hz (once every 20ms)
	
	// set PIN CHANGE INTERRUPT
	PCICR |= 1<<PCIE1 | 1<<PCIE2;		// enable pin change interrupts PC0-5 (PCI1) & PD3-4 (PCI2)
	PCMSK1 |= 1<<PCINT13 | 1<<PCINT12 | 1<<PCINT11 | 1<<PCINT10 | 1<<PCINT9 | 1<<PCINT8;	// enable pins PC0-5 (PCI1)
	PCMSK2 |= 1<<PCINT20 | 1<<PCINT19;														// enable pins PD3-4 (PCI2)
	
	// enable interrups
	sei();	
	
	// do nothing and repeat - interrupts do all the work
	for(;;);
}
