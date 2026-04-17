import random
import matplotlib.pyplot as plt
import math
import csv

# Create an individual with genotype and fitness


class Individual:
    def __init__(self, genotype):
        self.genotype = genotype
        self.fitness = 0


evaluations = 0

# Define the fitness function that we want to maximize


def deceptive_fitness(genotype, n):
    # Calculate the length of each segment
    segment_length = len(genotype) // n

    # Initialize the total fitness
    total_fitness = 0.0

    # Split the genotype into segments and calculate fitness for each
    for i in range(n):
        segment = genotype[i * segment_length: (i + 1) * segment_length]
        ones_count = segment.count(1)
        segment_fitness = ones_count
        if ones_count == 0:
            segment_fitness += segment_length + 1
        total_fitness += segment_fitness

    return total_fitness


def fitness_function(individual, histogram):
    #individual.fitness = individual.genotype.count(1)
    individual.fitness = deceptive_fitness(
        individual.genotype, 1)  # Calculate fitness

    genotype_string = "".join(map(str, individual.genotype))
    genotype_int = int(genotype_string, 2)
    if histogram[genotype_int]["fitness"] == 0:
        # Signal before marking it as tested
        # print(
        #    f"Testing a new genotype: {genotype_string} {individual.fitness}")
        histogram[genotype_int]["tests"] = 0
        histogram[genotype_int]["fitness"] = 0
        histogram[genotype_int]["genotype"] = genotype_string

    histogram[genotype_int]["tests"] += 1  # Update the tests count
    histogram[genotype_int]["fitness"] = individual.fitness

    global evaluations
    evaluations += 1


# Create an initial population of individuals


def create_individual(length):
    return Individual([random.randint(0, 1) for _ in range(length)])


# Uniform crossover (acts directly on parents)


def uniform_crossover(parent1, parent2, prob_uniform_crossover):
    for i in range(len(parent1.genotype)):
        if random.random() < prob_uniform_crossover:
            parent1.genotype[i], parent2.genotype[i] = (
                parent2.genotype[i],
                parent1.genotype[i],
            )


# Individual Mutation


def mutate(individual, mutation_rate):
    for i in range(len(individual.genotype)):
        if random.random() < mutation_rate:
            individual.genotype[i] = 1 - individual.genotype[i]  # Flip the bit


# Crossover Operator


def perform_crossovers(population, uniform_crossover_prob):
    for i in range(0, len(population), 2):
        if i + 1 < len(population):
            uniform_crossover(
                population[i], population[i + 1], uniform_crossover_prob)


# Mutation Operator


def perform_mutations(population, max_mut):
    exponent = math.log(len(population[0].genotype) * max_mut) / math.log(
        len(population)
    )
    for i in range(len(population)):
        base = i / len(population)
        mutation_rate = max_mut * base**exponent
        mutate(population[i], mutation_rate)


# Selection Operator
import math
import random

def select(population):
    """
    Select clones from a population based on rank-based probabilities.
    This function generates all clones after calculating the total number 
    needed for each individual, avoiding loops where possible.
    """
    # Step 1: Define constants and population size
    K = 3.0  # Cloning factor to control distribution
    population_size = len(population)

    # Step 2: Calculate ranks for each individual
    # Ranks are based on normalized indices (0 to 1)
    ranks = [i / population_size for i in range(population_size)]

    # Step 3: Calculate the number of clones (integer and fractional) for each individual
    # Integer part: Whole clones; Fractional part: Remaining probabilities for additional clones
    num_clones = [K * (1.0 - rank) ** (K - 1) for rank in ranks]
    integer_clones = list(map(math.floor, num_clones))
    fractional_clones = [num - int_clones for num, int_clones in zip(num_clones, integer_clones)]

    # Step 4: Determine additional clones needed to match population size
    additional_clones_needed = population_size - sum(integer_clones)

    # Step 5: Select indices for additional clones based on fractional probabilities
    # `random.choices` selects indices proportionally to their fractional probabilities
    additional_indices = random.choices(
        range(population_size), weights=fractional_clones, k=additional_clones_needed
    )

    # Step 6: Compute total clones for each individual
    # Sum integer clones and the count of additional clones for each index
    total_clones = [
        integer_clones[i] + additional_indices.count(i) for i in range(population_size)
    ]

    # Step 7: Generate all clones based on total_clones
    # For each individual, create the specified number of clones
    clones = [
        Individual(population[i].genotype[:]) for i in range(population_size) for _ in range(total_clones[i])
    ]

    # Step 8: Sort the clones by fitness in descending order
    # This ensures the fittest individuals appear first
    clones.sort(key=lambda ind: ind.fitness, reverse=True)

    return clones


# Evaluate the fitness of each individual in the population


def evaluate(population, histogram):
    for individual in population:
        fitness_function(individual, histogram)
    # Sort the individuals according to their fitness in descending order
    population.sort(key=lambda ind: ind.fitness, reverse=True)


# Genetic Algorithm parameters
population_size = 10
gene_length = 8
mutation_rate = 0.5
max_evaluations = 10000000

# Uniform Crossover probability (probability of exchanging a gene between parents)
uniform_crossover_prob = 0.5

# Initialize the population, genotype histogram, and the best known genotype
population = [create_individual(gene_length) for _ in range(population_size)]
histogram = [
    {"genotype": 0, "tests": 0, "fitness": 0.0} for _ in range(2**gene_length)
]
bestIndividual = None  # To track the best known individual


def check_improvements(population, phase):
    global bestIndividual
    if bestIndividual is None or population[0].fitness > bestIndividual.fitness:
        bestIndividual = population[0]
        print(f"{phase}: {bestIndividual.genotype} - Fitness: {bestIndividual.fitness}")


# Main loop for the genetic algorithm
evaluate(population, histogram)
check_improvements(population, "S")

while evaluations < max_evaluations:
    population = select(population)
    perform_crossovers(population, uniform_crossover_prob)

    evaluate(population, histogram)
    check_improvements(population, "R")

    perform_mutations(population, mutation_rate)

    evaluate(population, histogram)
    check_improvements(population, "M")

# Print the result
print("Best individual:", bestIndividual.genotype)
print("Fitness:", bestIndividual.fitness)

# Plot the histogram with all data, including zeros
# Sort the histogram data by fitness
histogram_sorted = sorted(
    [entry for entry in histogram if entry is not None],
    key=lambda entry: bin(histogram.index(entry)).count("1"),
    reverse=True,
)
print(len(histogram_sorted), evaluations)

# Plot the histogram sorted by fitness
plt.plot(range(len(histogram_sorted)), [
         entry["tests"] for entry in histogram_sorted])
plt.yscale("log")
plt.xlabel("Genotypes")
plt.ylabel("Counts")
plt.title("Rank GA")
plt.grid(True)  # Add grid lines
plt.show()

# Create a list of data to be written to the CSV file
csv_data = [["numOnes", "Genotype", "Tests", "Fitness"]]

for entry in histogram:
    if entry is not None:
        numOnes = entry["genotype"].count("1")
        genotype = entry["genotype"]
        tests = entry["tests"]
        fitness = entry["fitness"]
        csv_data.append([numOnes, "'" + genotype + "'", tests, fitness])

# Write the data to a CSV file
csv_file = "genotype_histogram.csv"
with open(csv_file, "w", newline="") as file:
    writer = csv.writer(file)
    writer.writerows(csv_data)

print(f"CSV file '{csv_file}' has been created.")
