#include <stdlib.h>
#include <stdio.h>
#include <time.h>

int main() {
	int i, r;
	srand(time(NULL));

	for(i = 0 ; i < 100; i++) {
		r = rand() % 100 + 1;
		printf("%d\n", r);
	} 

	return(0);
}
