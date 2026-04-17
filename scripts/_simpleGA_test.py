import random
import matplotlib.pyplot as plt
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
        individual.genotype, 2)  # Calculate fitness

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


# Genetic Algorithm parameters
population_size = 10
gene_length = 8
mutation_rate = 0.1
max_evaluations = 10000000

# Uniform Crossover probability (probability of selecting a gene from the first parent)
uniform_crossover_prob = 0.5

# Initialize the population, genotype histogram, and the best known genotype
population = [create_individual(gene_length) for _ in range(population_size)]
# Array to track how many times each genotype is tested
histogram = [
    {"genotype": 0, "tests": 0, "fitness": 0.0} for _ in range(2**gene_length)
]
best_individual = None  # To track the best known individual


# Main loop for the genetic algorithm
while evaluations < max_evaluations:
    # Evaluate the fitness of each individual in the population
    for individual in population:
        fitness_function(individual, histogram)
    # Sort the individuals according to their fitness in descending order
    population.sort(key=lambda ind: ind.fitness, reverse=True)

    # Check if the best individual improves the best known genotype
    if best_individual is None or population[0].fitness > best_individual.fitness:
        best_individual = population[0]
        print(
            f"Improved best known genotype: {best_individual.genotype} - Fitness: {best_individual.fitness}")

    # Select parents for reproduction based on fitness
    selected_parents = random.choices(
        population, weights=[score.fitness for score in population], k=population_size)

    # Create a new population through uniform crossover and mutation
    new_population = []
    new_population.append(best_individual)

    for _ in range(population_size-1):
        parent1, parent2 = random.choices(selected_parents, k=2)
        child = Individual('')
        child.genotype = [parent1.genotype[i] if random.random() < uniform_crossover_prob else parent2.genotype[i]
                          for i in range(gene_length)]

        # Apply mutation
        for i in range(gene_length):
            if random.random() < mutation_rate:
                child.genotype[i] = 1 - child.genotype[i]

        new_population.append(child)

    # Replace the old population with the new population
    population = new_population

# Print the result
print("Best individual:", best_individual.genotype, best_individual.fitness)

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
plt.title("Simple GA")
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
