import random
import math

class Individual:
    def __init__(self, fitness=None):
        if fitness is None:
            # If fitness is not provided, generate a random fitness value
            self.fitness = random.uniform(0.0, 1.0)
        else:
            self.fitness = fitness
        self.numClones = 0  # Initialize numClones for each individual


# Sample Population class, you should adapt this to your actual implementation
class Population:
    def __init__(self, K, population_size):
        self.K = K
        self.population_size = population_size
        self.theIndividuals = [Individual() for _ in range(population_size)]

    def calculate_num_clones(self):
        for i in range(len(self.theIndividuals)):
            r = i / (len(self.theIndividuals) - 0)
            numClones = self.K * (1 - r) ** (self.K - 1)
            self.theIndividuals[i].numClones = (numClones)
            print(i,round(numClones, 7))

    def sum_num_clones(self):
        return sum(individual.numClones for individual in self.theIndividuals)


# Sample value of K and population size
K = 3
population_size = 100

# Create a Population instance with K and population size
population = Population(K, population_size)

# Calculate numClones for each individual
population.calculate_num_clones()

# Sum all numClones
total_num_clones = population.sum_num_clones()

print("Sum of numClones:", total_num_clones)
