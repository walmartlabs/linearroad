#include <stdio.h>
#include <string.h>
#include <time.h>


// dataval.c: with a raw mitsim file perform the following:
//  check for position reports that are not 30 secs apart, and simply report
//  ensure car does not reappear after exiting
//  remove negative positions and segments
//  remove type 3 queries with a  day of '0' if any

// Usage: dataval.py <raw_file> <cleaner_file>


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

void write_tokens(FILE* fp, int* tokens, int size) {
	int i;
	for (i = 0; i < size ; i++) { 
		if (i == size - 1) {
			fprintf(fp, "%d", tokens[i]);
		} else {
			fprintf(fp, "%d,", tokens[i]);
		}
	}
	fprintf(fp, "\n");
}

int main(int argc, char* argv[]) {
	time_t st;
	st = time(NULL);
	int MAX_CARID = 300000;
	int NUM_FIELDS = 15;
	int i;
	int tokens[NUM_FIELDS];
	int cars[MAX_CARID];
	int exited[MAX_CARID];
	for (i = 0 ; i < MAX_CARID ; i++) {
		cars[i] = 0;
		exited[i] = 0;
	} 
	char* token;
	char* read_from_file = argv[1];
	char* write_to_file = argv[2];
	FILE* rf = fopen(read_from_file, "r");
	FILE* wt = fopen(write_to_file, "w");
	if (rf != NULL) {
		char line[100];
		int tokencount = 0;
		while (fgets(line, 100, rf) != NULL) {
			token = strtok(line, ",");
			while ( token != NULL) {
				tokens[tokencount] = atoi(token);
				token = strtok(NULL, ",");
				tokencount++;
			}
			tokencount = 0;
			// Do stuff here with the tokens, for each line
			if (tokens[0] == 0) {
				if (exited[tokens[2]] != 0) {
					continue;
				}
				if (cars[tokens[2]] == 0) {
					cars[tokens[2]] = tokens[1]; 
				} else {
					if (cars[tokens[2]] != tokens[1]-30) {
						printf("%d - %d\n", cars[tokens[2]], tokens[1]);
						printf("Time error for car %d at time %d\n", tokens[2], tokens[1]);	 
					}
					cars[tokens[2]] = tokens[1];
				}	
				if (tokens[5] == 4) { // Put this car in the exited data struct
					exited[tokens[2]] = tokens[1];
				}
				if (tokens[7] < 0) {
					print_tokens(tokens, NUM_FIELDS);	
					tokens[7] = 0;
					tokens[8] = 0; // Because we get odd (as in 'weird') negative segments
				}
			} else if (tokens[0] == 2) {
			} else if (tokens[0] == 3) {
				if (tokens[14] == 0) {
					continue;
				} 
			}	
			write_tokens(wt, tokens, NUM_FIELDS);
		}
	}	

	fclose(rf);
	fclose(wt);
	printf("Time to run dataval.c: %d\n", time(NULL) - st);	

	return(0);
}

