#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

int main(int argc, char* argv[]) {
	printf("Hello World!\n");
	int i;
	int tokens[10];
	printf("%lu\n", sizeof(tokens));
	printf("%lu\n", sizeof(int));
	printf("%lu\n", sizeof(tokens)/sizeof(int));
	for (i = 0 ; i < sizeof(tokens)/sizeof(int) ; i++) {
		printf("%d\n", i);
	}
    char line[] = "hello,world";
    char *token;
    token = strtok(line,",");
    //printf("%s\n", token[0]);
    //printf("%s\n", token[1]);
    while (token != NULL)
    {
        printf("%s\n", token);
        // Move to the next token.
        token = strtok(NULL, ",");
    }
    //printf("%s\n", token[0]);
    //printf("%s\n", token[1]);

    printf("Test sqrt.\n");
    for (i = 0 ; i <= 11; i++)
    {
        printf("(int)floor(sqrt(%d)) = %d\n", i, (int)floor(sqrt(i))); 
    }
    printf("1/2 = %d\n", 1/2);
    printf("2 << 3 = %d\n", 2 << 3);
	return(0);
}
