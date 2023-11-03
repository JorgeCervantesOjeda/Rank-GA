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


def select(population):
    # Calculate the rank for each individual
    ranks = [i / len(population) for i in range(len(population))]

    # Calculate the number of clones for each individual
    K = 3.0
    num_clones = [(K * (1.0 - rank) ** (K - 1)) for rank in ranks]

    # Produce clones based on ranks
    clones = []
    for i, num in enumerate(num_clones):
        clones.extend(
            [Individual(population[i].genotype[:])
             for _ in range(math.floor(num))]
        )

    # Calculate the fractional number of clones
    fractional_clones = [num - math.floor(num) for num in num_clones]

    # Produce extra clones based on fractional clones
    i = 0
    while len(clones) < len(population):
        if random.random() < fractional_clones[i]:
            clones.append(Individual(population[i].genotype[:]))
        # Increment i in a circular manner
        i = (i + 1) % len(population)

    # Sort the clones by fitness
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
