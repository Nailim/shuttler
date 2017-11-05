#include <avr/io.h>
#include <util/delay.h>
#include <avr/interrupt.h>

// USART definitions
#define BAUD 9600
#define FOSC 20000000			// clock Speed
#define MYUBRR FOSC/16/BAUD-1

// global variables
volatile unsigned char port_c_last = 0x00;		// pin change interrupts PC0-5
volatile unsigned char port_c_change = 0x00;	// pin change interrupts PC0-5
volatile unsigned char port_d_last = 0x00;		// pin change interrupts PD3-4
volatile unsigned char port_d_change = 0x00;	// pin change interrupts PD3-4

//volatile unsigned char timer_temp_low = 0x00;
//volatile unsigned char timer_temp_high = 0x00;
volatile unsigned short timer_temp = 0x0000;
volatile unsigned short timer_temp_result = 0x0000;
volatile unsigned short timer_temp_storage[8] = {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};

volatile unsigned char counter_low = 0x00;
volatile unsigned char counter_high = 0x00;

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
	// get the timer, get he timer NOW!
	//timer_temp_low = TCNT1L;
	//timer_temp_high = TCNT1H;
	timer_temp = TCNT1;
	
	// check which pin triggerd the interrupt
	port_c_change = (PINC & PCMSK1) ^ port_c_last;
	port_d_change = (PIND & PCMSK2) ^ port_d_last;
	
	// find and service the apropriate pins
	if (port_c_change & 0x20) {			// channel 1 (PC5 - PCINT 13 - PCMSK1 bit 5)
		//USART_Transmit('a');
		if (PINC & 0x20) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[0] = (timer_temp_high<<8);
			//timer_temp_storage[0] = timer_temp_storage[0] + timer_temp_low;
			timer_temp_storage[0] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[0];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_c_change & 0x10) {	// channel 2 (PC4 - PCINT 12 - PCMSK1 bit 4)
		//USART_Transmit('b');
		if (PINC & 0x10) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[1] = (timer_temp_high<<8);
			//timer_temp_storage[1] = timer_temp_storage[1] + timer_temp_low;
			timer_temp_storage[1] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[1];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_c_change & 0x08) {	// channel 3 (PC3 - PCINT 11 - PCMSK1 bit 3)
		//USART_Transmit('c');
		if (PINC & 0x08) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[2] = (timer_temp_high<<8);
			//timer_temp_storage[2] = timer_temp_storage[2] + timer_temp_low;
			timer_temp_storage[2] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[2];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_c_change & 0x04) {	// channel 4 (PC2 - PCINT 10 - PCMSK1 bit 2)
		//USART_Transmit('d');
		if (PINC & 0x04) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[3] = (timer_temp_high<<8);
			//timer_temp_storage[3] = timer_temp_storage[3] + timer_temp_low;
			timer_temp_storage[3] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[3];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_c_change & 0x02) {	// channel 5 (PC1 - PCINT 9 - PCMSK1 bit 1)
		//USART_Transmit('e');
		if (PINC & 0x02) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[4] = (timer_temp_high<<8);
			//timer_temp_storage[4] = timer_temp_storage[4] + timer_temp_low;
			timer_temp_storage[4] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[4];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_c_change & 0x01) {	// channel 6 (PC0 - PCINT 8 - PCMSK1 bit 0)
		//USART_Transmit('f');
		if (PINC & 0x01) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[5] = (timer_temp_high<<8);
			//timer_temp_storage[5] = timer_temp_storage[5] + timer_temp_low;
			timer_temp_storage[5] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[5];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_d_change & 0x08) {	// channel 7 (PD3 - PCINT 19 - PCMSK2 bit 3)
		//USART_Transmit('g');
		if (PIND & 0x08) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[6] = (timer_temp_high<<8);
			//timer_temp_storage[6] = timer_temp_storage[6] + timer_temp_low;
			timer_temp_storage[6] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[6];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	else if (port_d_change & 0x10) {	// channel 8 (PD4 - PCINT 20 - PCMSK2 bit 4)
		//USART_Transmit('h');
		if (PIND & 0x10) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[7] = (timer_temp_high<<8);
			//timer_temp_storage[7] = timer_temp_storage[7] + timer_temp_low;
			timer_temp_storage[7] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			timer_temp_result = timer_temp - timer_temp_storage[7];
			USART_Transmit(timer_temp_result>>8);
			USART_Transmit(timer_temp_result);
		}
	}
	//if (PINC & 0x20) {			// channel 1 (PC5)
	//	
	//}
	
	
	// save port state
	port_c_last = (PINC & PCMSK1);
	port_d_last = (PIND & PCMSK2);
	
	//USART_Transmit('p');
	//USART_Transmit('p');
	//USART_Transmit('p');
	//USART_Transmit('\n');
	//PORTD |= 0x04;
	//if (PIND & 0x10) {
	//	USART_Transmit('+');
	//	USART_Transmit('+');
	//	USART_Transmit('+');
	//	USART_Transmit('\n');
	//}
	//else {
	//	USART_Transmit('-');
	//	USART_Transmit('-');
	//	USART_Transmit('-');
	//	USART_Transmit('\n');
	//}
	//port_c_change = (PINC & PCMSK1) ^ port_c_last;
	//port_d_change = (PIND & PCMSK2) ^ port_d_last;
	
	//USART_Transmit(port_c_change);
	//USART_Transmit(port_d_change);
	//USART_Transmit('\n');
	
	/*
	USART_Transmit('@');
	USART_Transmit('@');
	if (PIND & 0x10) {
		USART_Transmit('+');
		USART_Transmit('+');
		USART_Transmit('+');
		USART_Transmit('+');
	}
	else {
		USART_Transmit('-');
		USART_Transmit('-');
		USART_Transmit('-');
		USART_Transmit('-');
	}
	*/
	//USART_Transmit(timer_temp_high);
	//USART_Transmit(timer_temp_low);
	//USART_Transmit('\n');
	
	//port_c_last = (PINC & PCMSK1);
	//port_d_last = (PIND & PCMSK2);
}

//ISR(PCINT2_vect) {
//	USART_Transmit('p');
//	USART_Transmit('p');
//	USART_Transmit('p');
//	USART_Transmit('\n');
//	//PORTD |= 0x04;
//}

//ISR(TIMER2_COMPA_vect) {
//	// update the conte, update the counter now
//	counter_low++;
//	
//	// send 5 times per second
//	if (counter_low >= 125) {
//		// transmmit
//		//USART_Transmit('@');
//		//USART_Transmit('@');
//		
//		counter_low = 0;
//		counter_high++;
//		
//		if (counter_high == 5) {
//			//PORTD |= 0x04;
//		}
//		else if (counter_high >= 10) {
//			//PORTD &= ~0x04;
//			counter_high = 0;
//		}
//	}
//	
//}

ISR(TIMER1_COMPB_vect) {
	// update the conte, update the counter now
	counter_low++;
	
	// run 25 times per second - transmit and more
	if (counter_low >= 50) {
		// transmmit
		//USART_Transmit('@');
		//USART_Transmit('@');
		
		counter_low = 0;
		counter_high++;
		
		// run 1 time per second - set servo tester position and LED status
		if (counter_high == 1) {
			OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
		}
		else if (counter_high == 2) {
			OCR1A = (2500*1.0);		// servo left position
			PORTD &= ~0x04;			// LED off
		}
		else if (counter_high == 3) {
			OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
		}
		else if (counter_high == 4) {
			OCR1A = (2500*2.0);		// servo right position
			PORTD &= ~0x04;			// LED off
			counter_high = 0;		// restart the cycle
		}
		else {
			counter_high = 0;		// something has gone horrobly wrong - restart the cycle
		}
	}
	
}

int main(void) {
	
	// set PD2 as output - LED
	DDRD |= 0x04;
	PORTD &= ~0x04;		// LED (PD2) off
	
	// set PB1 as output - PWM
DDRB |= 0x02;
	
	// set PC0-5 (PCI1) & PD3-4 (PCI2) as input - PIN CHANGE INTERRUPT
	DDRC &= ~0x63;
	DDRD &= ~0x20;	// leave out the LED
	
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
	
	
//	// set Timer/Counter2 with interrupt
//	TCCR2A |= 1<<WGM21;					// set Clear Timer on Comapre match
//	TCCR2B |= 1<<CS22 | 1<<CS21;		// set prescaler to 256 (20000000/256) = 78125 ticks per second
//	TIMSK2 |= 1<<OCIE2A;				// set interrupt on CTC
//	OCR2A = 125;						// set Output Compare Register for (78125/125) = 625Hz
	
	// set PIN CHANGE INTERRUPT
	PCICR |= 1<<PCIE1 | 1<<PCIE2;		// enable pin change interrupts PC0-5 (PCI1) & PD3-4 (PCI2)
	PCMSK1 |= 1<<PCINT13 | 1<<PCINT12 | 1<<PCINT11 | 1<<PCINT10 | 1<<PCINT9 | 1<<PCINT8;	// enable pins PC0-5 (PCI1)
	PCMSK2 |= 1<<PCINT20 | 1<<PCINT19;														// enable pins PD3-4 (PCI2)
	//MCUCR = (1<<ISC01) | (1<<ISC00);
	
	// enable interrups
	sei();	
	
	// endless loop of nothingness - interrupts do all the work
	
	for(;;) {
		asm("nop");		// do nothing and repeat
	}
}
