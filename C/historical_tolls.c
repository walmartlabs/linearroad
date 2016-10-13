#include <stdio.h>
#include <stdlib.h>
#include <time.h>

// Author: Sung Kim
//
// Description: This is a single-threaded C version.
// Toll generation for 1M cars with 200 XWays takes ~21 seconds.
// On the same HW the original perl version took ~45 seconds.
// On the same HW the Java version takes ~15 seconds.
// On the same HW the Java version, but using System.out instead of PrintWriter takes ~4 min.
// On the same HW the C version, using fprintf on a file instead of printf and redirection takes ~46 sec.



const int NUM_DAYS_IN_HISTORY = 70;

int main(int argc, char* argv[])
{
    if (argc != 4)
    {
        printf("Usage: ./historical_tolls <Max XWay> <Max CarId> <Output file>\n");
        printf("<Max XWay> is [0, max] and <Max CarId> is [0, max) \n");
        return 1;
    }

	int max_xway = atoi(argv[1]);
	int max_carid = atoi(argv[2]);
	char *outfile = argv[3];

	printf("Max XWay: %d\n", max_xway);
	printf("Max CarId: %d\n", max_carid);
	printf("Outfile: %s\n", outfile);

	int i, day, toll, xway;
	//FILE *writer;

	/*
	writer = fopen(outfile, "w");
	if (writer == NULL)
	{
	    fprintf(stderr, "Can't open file %s for writing. Exiting.\n", outfile);
	    return 1;
	}
	*/

	srand(time(NULL));

	max_xway++;  // We do this we when we % we get 0-max_xway xways.

	for (i = 0 ; i < max_carid ; i++)
	{
		for (day = 1 ; day < NUM_DAYS_IN_HISTORY ; day++)
		{
            clock_t time4rand = clock();
			toll = rand() % 90 + 10;  // The +1 gives us up to 99 and removes 0 tolls
			xway = rand() % max_xway;  // The original was ] (inclusive, so this one is too)
			printf("%d,%d,%d,%d\n",i,day,xway,toll);
			//fprintf(writer, "%d,%d,%d,%d\n",i,day,xway,toll);
			//printf("Time to create random toll and random xway and print it%f\n",
            //			((double) (clock() - time4rand)) / CLOCKS_PER_SEC);
		}
	}

	//fclose(writer);

	return 0;
}


