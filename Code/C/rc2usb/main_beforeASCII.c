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

//volatile unsigned char timer_temp_low = 0x00;
//volatile unsigned char timer_temp_high = 0x00;
volatile unsigned short timer_temp = 0x0000;
volatile unsigned short timer_temp_result[8] = {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};
volatile unsigned short timer_temp_storage[8] = {0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000};

volatile unsigned char counter_low = 0x00;
volatile unsigned char counter_high = 0x00;
volatile unsigned short temp_storage = 0x0000;

volatile float data_out = 0.0;											// output data in the right format for flightgear to parse
volatile unsigned char *data_out_handler = (unsigned char*)&data_out;	// pointer to output data, so it can be transmitted byte by byte

volatile float testing_out = -0.9;
char outout[10];
unsigned char ind;

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
	//timer_temp_low = TCNT1L;
	//timer_temp_high = TCNT1H;
	
	
	// check which pin triggerd the interrupt
	port_c_change = (PINC & PCMSK1) ^ port_c_last;
	port_d_change = (PIND & PCMSK2) ^ port_d_last;
	port_c_last = (PINC & PCMSK1);
	port_d_last = (PIND & PCMSK2);
	
	// find and service the apropriate pins
	if (port_c_change & 0x20) {			// channel 1 (PC5 - PCINT 13 - PCMSK1 bit 5)
		//USART_Transmit('a');
		if (port_c_last & 0x20) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[0] = (timer_temp_high<<8);
			//timer_temp_storage[0] = timer_temp_storage[0] + timer_temp_low;
			timer_temp_storage[0] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[0]) {
				timer_temp_result[0] = timer_temp - timer_temp_storage[0];
			}
			else {
				timer_temp_result[0] = (ICR1 - timer_temp_storage[0]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[0]>>8);
			//USART_Transmit(timer_temp_result[0]);
		}
	}
	if (port_c_change & 0x10) {	// channel 2 (PC4 - PCINT 12 - PCMSK1 bit 4)
		//USART_Transmit('c');
		if (port_c_last & 0x10) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[1] = (timer_temp_high<<8);
			//timer_temp_storage[1] = timer_temp_storage[1] + timer_temp_low;
			timer_temp_storage[1] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[1]) {
				timer_temp_result[1] = timer_temp - timer_temp_storage[1];
			}
			else {
				timer_temp_result[1] = (ICR1 - timer_temp_storage[1]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[1]>>8);
			//USART_Transmit(timer_temp_result[1]);
		}
	}
	if (port_c_change & 0x08) {	// channel 3 (PC3 - PCINT 11 - PCMSK1 bit 3)
		//USART_Transmit('e');
		if (port_c_last & 0x08) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[2] = (timer_temp_high<<8);
			//timer_temp_storage[2] = timer_temp_storage[2] + timer_temp_low;
			timer_temp_storage[2] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[2]) {
				timer_temp_result[2] = timer_temp - timer_temp_storage[2];
			}
			else {
				timer_temp_result[2] = (ICR1 - timer_temp_storage[2]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[2]>>8);
			//USART_Transmit(timer_temp_result[2]);
		}
	}
	if (port_c_change & 0x04) {	// channel 4 (PC2 - PCINT 10 - PCMSK1 bit 2)
		//USART_Transmit('g');
		if (port_c_last & 0x04) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[3] = (timer_temp_high<<8);
			//timer_temp_storage[3] = timer_temp_storage[3] + timer_temp_low;
			timer_temp_storage[3] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[3]) {
				timer_temp_result[3] = timer_temp - timer_temp_storage[3];
			}
			else {
				timer_temp_result[3] = (ICR1 - timer_temp_storage[3]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[3]>>8);
			//USART_Transmit(timer_temp_result[3]);
		}
	}
	if (port_c_change & 0x02) {	// channel 5 (PC1 - PCINT 9 - PCMSK1 bit 1)
		//USART_Transmit('i');
		if (port_c_last & 0x02) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[4] = (timer_temp_high<<8);
			//timer_temp_storage[4] = timer_temp_storage[4] + timer_temp_low;
			timer_temp_storage[4] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[4]) {
				timer_temp_result[4] = timer_temp - timer_temp_storage[4];
			}
			else {
				timer_temp_result[4] = (ICR1 - timer_temp_storage[4]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[4]>>8);
			//USART_Transmit(timer_temp_result[4]);
		}
	}
	if (port_c_change & 0x01) {	// channel 6 (PC0 - PCINT 8 - PCMSK1 bit 0)
		//USART_Transmit('k');
		if (port_c_last & 0x01) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[5] = (timer_temp_high<<8);
			//timer_temp_storage[5] = timer_temp_storage[5] + timer_temp_low;
			timer_temp_storage[5] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[5]) {
				timer_temp_result[5] = timer_temp - timer_temp_storage[5];
			}
			else {
				timer_temp_result[5] = (ICR1 - timer_temp_storage[5]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[5]>>8);
			//USART_Transmit(timer_temp_result[5]);
		}
	}
	if (port_d_change & 0x08) {	// channel 7 (PD3 - PCINT 19 - PCMSK2 bit 3)
		//USART_Transmit('m');
		if (port_d_last & 0x08) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[6] = (timer_temp_high<<8);
			//timer_temp_storage[6] = timer_temp_storage[6] + timer_temp_low;
			timer_temp_storage[6] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[6]) {
				timer_temp_result[6] = timer_temp - timer_temp_storage[6];
			}
			else {
				timer_temp_result[6] = (ICR1 - timer_temp_storage[6]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[6]>>8);
			//USART_Transmit(timer_temp_result[6]);
		}
	}
	if (port_d_change & 0x10) {	// channel 8 (PD4 - PCINT 20 - PCMSK2 bit 4)
		//USART_Transmit('o');
		if (port_d_last & 0x10) {				// low to hight
			//USART_Transmit('+');
			//timer_temp_storage[7] = (timer_temp_high<<8);
			//timer_temp_storage[7] = timer_temp_storage[7] + timer_temp_low;
			timer_temp_storage[7] = timer_temp;
		}
		else {							// high to low
			//USART_Transmit('-');
			//timer_temp = (timer_temp_high<<8);
			//timer_temp = timer_temp + timer_temp_low;
			
			if (timer_temp >= timer_temp_storage[7]) {
				timer_temp_result[7] = timer_temp - timer_temp_storage[7];
			}
			else {
				timer_temp_result[7] = (ICR1 - timer_temp_storage[7]) + timer_temp;
			}
			
			//USART_Transmit(timer_temp_result[7]>>8);
			//USART_Transmit(timer_temp_result[7]);
		}
	}	
	
	// save port state
	// port_c_last = (PINC & PCMSK1);
	// port_d_last = (PIND & PCMSK2);
}

ISR(TIMER1_COMPB_vect) {
	cli();
	// update the conter, update the counter now
	counter_low++;
	
	// run 25 times per second - transmit and more
	if (counter_low >= 2) {
		// transmmit
		
		//USART_Transmit(timer_temp_result[0]>>8);
		//USART_Transmit(timer_temp_result[0]);
		//USART_Transmit(timer_temp_result[1]>>8);
		//USART_Transmit(timer_temp_result[1]);
		//USART_Transmit(timer_temp_result[2]>>8);
		//USART_Transmit(timer_temp_result[2]);
		//USART_Transmit(timer_temp_result[3]>>8);
		//USART_Transmit(timer_temp_result[3]);
		//USART_Transmit(timer_temp_result[4]>>8);
		//USART_Transmit(timer_temp_result[4]);
		//USART_Transmit(timer_temp_result[5]>>8);
		//USART_Transmit(timer_temp_result[5]);
		//USART_Transmit(timer_temp_result[6]>>8);
		//USART_Transmit(timer_temp_result[6]);
		//USART_Transmit(timer_temp_result[7]>>8);
		//USART_Transmit(timer_temp_result[7]);	
		
		temp_storage = timer_temp_result[0];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		//data_out = testing_out;
		dtostrf(data_out, 0, 2, outout);
		
		ind = 0;
		
		while (outout[ind] != '\0') {
			USART_Transmit(outout[ind]);
			ind++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[1];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		//data_out = testing_out;
		dtostrf(data_out, 0, 2, outout);
		
		ind = 0;
		
		while (outout[ind] != '\0') {
			USART_Transmit(outout[ind]);
			ind++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[2];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		//data_out = testing_out;
		dtostrf(data_out, 0, 2, outout);
		
		ind = 0;
		
		while (outout[ind] != '\0') {
			USART_Transmit(outout[ind]);
			ind++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[3];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		//data_out = testing_out;
		dtostrf(data_out, 0, 2, outout);
		
		ind = 0;
		
		while (outout[ind] != '\0') {
			USART_Transmit(outout[ind]);
			ind++;	
		}
		USART_Transmit('\t');
		
		temp_storage = timer_temp_result[4];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		//data_out = testing_out;
		dtostrf(data_out, 0, 2, outout);
		
		ind = 0;
		
		while (outout[ind] != '\0') {
			USART_Transmit(outout[ind]);
			ind++;	
		}
		USART_Transmit('\n');
		/*
		temp_storage = timer_temp_result[0];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		data_out = testing_out;
		//data_out_handler = (unsigned char*)&data_out;
		USART_Transmit(data_out_handler[3]);
		USART_Transmit(data_out_handler[2]);
		USART_Transmit(data_out_handler[1]);
		USART_Transmit(data_out_handler[0]);
		*/
		
		/*
		temp_storage = timer_temp_result[1];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		data_out = testing_out;
		//data_out_handler = (unsigned char*)&data_out;
		USART_Transmit(data_out_handler[3]);
		USART_Transmit(data_out_handler[2]);
		USART_Transmit(data_out_handler[1]);
		USART_Transmit(data_out_handler[0]);
		*/
		
		/*
		temp_storage = timer_temp_result[2];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		data_out = testing_out;
		//data_out_handler = (unsigned char*)&data_out;
		USART_Transmit(data_out_handler[3]);
		USART_Transmit(data_out_handler[2]);
		USART_Transmit(data_out_handler[1]);
		USART_Transmit(data_out_handler[0]);
		*/
		
		/*
		temp_storage = timer_temp_result[4];
		data_out = (temp_storage / 1250.0) - 3.0 ;
		data_out = testing_out;
		//data_out_handler = (unsigned char*)&data_out;
		USART_Transmit(data_out_handler[3]);
		USART_Transmit(data_out_handler[2]);
		USART_Transmit(data_out_handler[1]);
		USART_Transmit(data_out_handler[0]);
		*/
		
		testing_out = testing_out + 0.005;
		if (testing_out > 0.9){
			testing_out = -0.9;
		}
		
		counter_low = 0;
		counter_high++;
		
		// run 1 time per second - set servo position and LED status
		if (counter_high == 25) {
			OCR1A = (2500*1.0);		// servo left position
			//OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
			
			/*
			test = 0.0;
			unsigned char *d = (unsigned char*)&test;
			USART_Transmit(d[3]);
			USART_Transmit(d[2]);
			USART_Transmit(d[1]);
			USART_Transmit(d[0]);
			*/
		}
		else if (counter_high == 50) {
			OCR1A = (2500*1.0);		// servo left position
			PORTD &= ~0x04;			// LED off
			
			/*
			test = 1.0;
			unsigned char *d = (unsigned char*)&test;
			USART_Transmit(d[3]);
			USART_Transmit(d[2]);
			USART_Transmit(d[1]);
			USART_Transmit(d[0]);
			*/
		}
		else if (counter_high == 75) {
			OCR1A = (2500*1.0);		// servo left position
			//OCR1A = (2500*1.5);		// servo center position
			PORTD |= 0x04;			// LED on
			
			/*
			test = 0.0;
			unsigned char *d = (unsigned char*)&test;
			USART_Transmit(d[3]);
			USART_Transmit(d[2]);
			USART_Transmit(d[1]);
			USART_Transmit(d[0]);
			*/
		}
		else if (counter_high == 100) {
			OCR1A = (2500*1.0);		// servo left position
			//OCR1A = (2500*2.0);		// servo right position
			PORTD &= ~0x04;			// LED off
			counter_high = 0;		// restart the cycle
			
			/*
			test = -1.0;
			unsigned char *d = (unsigned char*)&test;
			USART_Transmit(d[3]);
			USART_Transmit(d[2]);
			USART_Transmit(d[1]);
			USART_Transmit(d[0]);
			*/
		}
		/*
		else {
			counter_high = 0;		// something has gone horrobly wrong - restart the cycle
		}
		*/
	}
	sei();
}

//ISR(TIMER2_COMPA_vect) {
//	// update the conter, update the counter now
//	counter_low++;
//	
//	// run 25 times per second - transmit and more
//	if (counter_low >= 125) {
//		// transmmit
//		
//		counter_low = 0;
//		counter_high++;
//		// run 1 time per second - set servo position and LED status
//		if (counter_high == 25) {
//			// more
//			counter_high = 0;
//		}
//	}
//}

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
	
//	// set Timer/Counter2 with interrupt
//	TCCR2A |= 1<<WGM21;					// set Clear Timer on Comapre match
//	TCCR2B |= 1<<CS22 | 1<<CS21;		// set prescaler to 256 (20000000/256) = 78125 ticks per second
//	TIMSK2 |= 1<<OCIE2A;				// set interrupt on CTC
//	OCR2A = 125;						// set Output Compare Register for (78125/125) = 625Hz
	
	// set PIN CHANGE INTERRUPT
	PCICR |= 1<<PCIE1 | 1<<PCIE2;		// enable pin change interrupts PC0-5 (PCI1) & PD3-4 (PCI2)
	PCMSK1 |= 1<<PCINT13 | 1<<PCINT12 | 1<<PCINT11 | 1<<PCINT10 | 1<<PCINT9 | 1<<PCINT8;	// enable pins PC0-5 (PCI1)
	PCMSK2 |= 1<<PCINT20 | 1<<PCINT19;														// enable pins PD3-4 (PCI2)
	
	//while(1){
	//	USART_Transmit(0xaa);				
	//}
	
	// enable interrups
	sei();	
	
	// do nothing and repeat - interrupts do all the work
	for(;;);
}
