#include <stdio.h>
#include <string.h>


int MAX_CARID = 300000;
int NUM_FIELDS = 15;
int LINE_BUF_SIZE = 100;

// datamakeexit.c: ensure that all vehicles get off the xway
//  run after dataval.c and datarm2.c
// Usage: datamakeexit <file> > <outfile>

void print_tokens(int* tokens, int size) {
	int i;
	for (i = 0; i < size ; i++) { 
		if (i == size - 1) {
			printf("%d", tokens[i]);
		} else {
			printf("%d,", tokens[i]);
		}
	}
	printf("\n");
}

void tokenize(int* tokens, char* line) {
	char* token;
	int tokencount = 0;
	token = strtok(line, ",");
	while(token != NULL) {
		tokens[tokencount] = atoi(token);
		token = strtok(NULL, ",");
		tokencount++;
	}
}
			
int main(int argc, char* argv[]) {
	FILE* fp;
	int i;
	int tokens[NUM_FIELDS];
	char line[LINE_BUF_SIZE];

	int lasttimes[MAX_CARID];

	fp = fopen(argv[1], "r");
	if (fp == NULL) {
		printf("Bad file handle\n");
		return(1);
	}

	while(fgets(line, LINE_BUF_SIZE, fp) != NULL) {
		tokenize(tokens, line);	
		lasttimes[tokens[2]] = tokens[1];
	} 
	fclose(fp);
	
	fp = fopen(argv[1], "r");
	if (fp == NULL) {
		printf("Bad file handle\n");
		return(1);
	}

	while(fgets(line, LINE_BUF_SIZE, fp) != NULL) {
		tokenize(tokens, line);	
		if (tokens[1] == lasttimes[tokens[2]] && tokens[0] == 0) { // only last appearing type 0 queries need adjustment 
			tokens[3] = 10;
			tokens[5] = 4;
			print_tokens(tokens, NUM_FIELDS);	
		} else {
			print_tokens(tokens, NUM_FIELDS);	
		}
	} 
	

	return(0);
}

