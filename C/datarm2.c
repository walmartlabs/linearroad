#include <stdlib.h>
#include <string.h>
#include <stdio.h>


// @ http://www.yolinux.com/TUTORIALS/CppStlMultiMap.html
// @ http://en.cppreference.com/w/cpp/string/byte/strtok

// datarm2.cc: remove carid's with only one or two records
// writes to stdout
// Usage: ./datarm2 <file> > <outfile>
// A c++ program to access the sTL map

// #########################################
// Don't bother with maps.  Use C and arrays
// #########################################

int main(int argc, char* argv[]) {
	char* file_name = argv[1];
	//printf("File name is: %s\n", argv[1]);
	char* token;
	int i;
	int tokens[15];
	char buf[100];
	int MAX_CARID = 300000;
	int counts[MAX_CARID]; // This will serve as out mapping
	//printf("Initializing counts ...\n");	
	for (i = 0 ; i < MAX_CARID ; i++) {
		counts[i] = 0;
	} 
	//printf("Done initializing counts ...\n");	

	FILE* fp = fopen(file_name, "r");
		
	if (fp != NULL) {
		//printf("Reading file ...\n");
		char line[100];
		int tokencount = 0;
		while (fgets(line, 100, fp) != NULL) {
			token = strtok(line, ",");
			while ( token != NULL) {
				tokens[tokencount] = atoi(token);
				token = strtok(NULL, ",");
				tokencount++;
			}
			tokencount = 0;
			if (counts[tokens[2]] == 0) {
				counts[tokens[2]] = 1; 
			} else {
				counts[tokens[2]] += 1;
			}
		}
	}
	fclose(fp);

	fp = fopen(file_name, "r");
	if (fp != NULL) {
		//printf("Finding valid cars...\n");
		char line[100];
		int tokencount = 0;
		while (fgets(line, 100, fp) != NULL) {
			token = strtok(line, ",");
			while ( token != NULL) {
				tokens[tokencount] = atoi(token);
				token = strtok(NULL, ",");
				tokencount++;
			}
			tokencount = 0;
			if (counts[tokens[2]] != 0) { // carid is present
				if (counts[tokens[2]] > 2) {  // has > 2 records
					if (tokens[0] != 4) { // ignore type 4's
						if (tokens[0] == 3) { // a redundant check after dataval.c, but check anyways for day == 0 for type == 3
							if (tokens[14] == 0) {
								continue;
							}
						}
						for (i = 0; i < sizeof(tokens)/sizeof(int) ; i++) { 
							if (i == sizeof(tokens)/sizeof(int) - 1) {
								printf("%d", tokens[i]);
							} else {
								printf("%d,", tokens[i]);
							}
						}
						printf("\n");
					}
				}	
			}
		}
	}
	fclose(fp);
	
	return(0);
}

