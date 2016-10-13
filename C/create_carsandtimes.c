#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <time.h>

// Author: Sung Kim
// Create the carsandtimes after reading the single combined file
// ********************************************************
// This only creates the carsandtimes NOT the carstoreplace
// ********************************************************
// Usage: ./create_carsandtimes <combined file> <overlap factor: 0 - 100> <carsandtimes_outfile>

#define INITIAL_SIZE 100

// The structure to hold the relevant information for a car.
typedef struct carandtime
{
    int carid;
    int enter;
    int exit;
    int xway;
}
carandtime;

// Function to increase the size of an array on the heap.
void doublearr(int *arr, size_t size)
{
    int *temp = (int*) malloc(sizeof(int) * size * 2);
    memcpy(temp, arr, sizeof(int) * size);
    free(arr);
    arr = temp;
}

// Functions to turn an array of ints into a binary tree.
// Add.
int add(int *arr, int val, size_t size)
{

}

// Find.
int find(int *arr, int val, size_t size)
{
}

int main(int argc, char *argv[])
{

    char *infile = argv[0];
    int overlap = atoi(argv[1]);
    char *outfile = argv[2];

    // Normalize the rand()
    const int NORM_MAX = 100;
    int normalunit = RAND_MAX/NORM_MAX;

    // Hold the [carid, ["Enter"|"Exit"|"Xway":value]].
    // Use a struct for simplicity.
    carandtime *cars;
    // Since we're choosing only a percentage of cars to be candidate cars.
    int *rejects;

    size_t size = INITIAL_SIZE;
    // Start the sizes of the arrays with the default size.
    size_t cars_size = size;
    size_t rejects_size = size;

    cars = (carandtime*) malloc(sizeof(carandtime) * size);
    rejects = (int*) malloc(sizeof(int) * size);

    FILE *reader fopen(infile, "r");
    FILE *writer fopen(outfile, "w"); // Simple printf with redirection will be faster.

    // Hold the string array of tokens.
    char *token;
    // Hold the int array of tokens.
    // We only need carid (pos. 2), time (pos. 1), and xway (pos. 4) plus the others as placeholders, type (pos. 0)
    // and speed (pos. 3).
    int tokens[5];
    int carid, time, xway;
    int r;

    // Read th combined file and build out the cars and times (carid [entertime, exittime, xway]).
    // Open the file.
    FILE* fp = fopen(infile, "r");
    srand(time(NULL));
    // Ensure file exists or is otherwise readable.
   	if (fp != NULL)
   	{
   	    // Hold each line of the file as we read it.
        char line[100];
        // For building out the int representation of the array.
        int tokencount = 0;
        // Read the file line by line.
        while (fgets(line, 100, fp) != NULL)
        {
            // Tokenize the line.
            token = strtok(line, ",");
            // Convert the array of strings to ints.
            while (token != NULL || tokencount < 5)
            {
                tokens[tokencount] = atoi(token);
                // Move to the next token.
                token = strtok(NULL, ",");
                tokencount++;
            }
            // Reset the tokencount.
            tokencount = 0;

            carid = tokens[2];
            time = tokens[1];
            xway = tokens[4];

            // Don't bother to check the cars that are already rejected.
            if (find(rejects, carid) == -1)
            {
                // Update each exit time with the latest, if the car is not previously rejected.
                carandtime *temp = find(cars, carid);
                if (temp)
                {
                    temp->exit = time;
                }
                // A new car, never before seen. Check if this car will be a candiate.
                else
                {
                    if (rand() < normalunit * overlap)
                    {
                        carandtime *newcarandtime = (carandtime*) malloc(sizeof(carandtime));
                        newcarandtime->enter = time;
                        newcarandtime->exit = time;
                        newcarandtime->xway = xway;
                        add(cars, newcarandtime);
                    }
                    else
                    {
                        add(rejects, carid);
                    }
                }
            }
        }
    }

    // For all the created candidate cars, print out the carsandtimes file.
    for (int cid : cars.keySet())
    {
        writer.println(cid +","+cars.get(cid).get("Enter")+","+cars.get(cid).get("Exit")+","+cars.get(cid).get("Xway"));
    }
    writer.close();
}
