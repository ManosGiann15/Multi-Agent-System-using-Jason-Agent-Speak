import matplotlib.pyplot as plt
import pandas as pd
import numpy as np

def plot_histograms_with_stats_and_total(csv_file):
    """
    Reads a CSV file, cleans the data, and plots two histograms:
    1. Combined histogram for Agent1, Agent2, and Agent3 points with stats.
    2. Histogram for Total points with stats.
    """
    # Read the CSV file into a DataFrame
    df = pd.read_csv(csv_file, header=None, names=['Agent1', 'Agent2', 'Agent3', 'Total'])

    # Convert data to numeric, forcing errors to NaN and dropping them
    for col in ['Agent1', 'Agent2', 'Agent3', 'Total']:
        df[col] = pd.to_numeric(df[col], errors='coerce')
    df.dropna(subset=['Agent1', 'Agent2', 'Agent3', 'Total'], inplace=True)

    # Extract data for the agents and Total
    agent1_values = df['Agent1']
    agent2_values = df['Agent2']
    agent3_values = df['Agent3']
    total_values = df['Total']

    # Calculate mean and std for each agent and Total
    agent1_mean, agent1_std = agent1_values.mean(), agent1_values.std()
    agent2_mean, agent2_std = agent2_values.mean(), agent2_values.std()
    agent3_mean, agent3_std = agent3_values.mean(), agent3_values.std()
    total_mean, total_std = total_values.mean(), total_values.std()

    # Create a figure with two subplots
    fig, axes = plt.subplots(1, 2, figsize=(16, 6))

    # Plot the combined histogram for Agent1, Agent2, and Agent3
    axes[0].hist(agent1_values, bins=10, alpha=0.7, label='Agent1', edgecolor='black')
    axes[0].hist(agent2_values, bins=10, alpha=0.7, label='Agent2', edgecolor='black')
    axes[0].hist(agent3_values, bins=10, alpha=0.7, label='Agent3', edgecolor='black')

    # Add stats text to the combined histogram
    stats_text_agents = (
        f"Agent1: μ={agent1_mean:.2f}, σ={agent1_std:.2f}\n"
        f"Agent2: μ={agent2_mean:.2f}, σ={agent2_std:.2f}\n"
        f"Agent3: μ={agent3_mean:.2f}, σ={agent3_std:.2f}"
    )
    axes[0].text(
        0.95, 0.95, stats_text_agents, fontsize=10, transform=axes[0].transAxes,
        verticalalignment='top', horizontalalignment='right',
        bbox=dict(facecolor='white', alpha=0.8, edgecolor='black')
    )
    axes[0].set_title('Combined Histogram of Agent Points')
    axes[0].set_xlabel('Points')
    axes[0].set_ylabel('Frequency')
    axes[0].legend(loc='upper left')
    axes[0].grid(True)

    # Plot the histogram for Total points
    axes[1].hist(total_values, bins=10, alpha=0.7, edgecolor='black')

    # Add stats text to the Total histogram
    stats_text_total = f"Total: μ={total_mean:.2f}, σ={total_std:.2f}"
    axes[1].text(
        0.95, 0.95, stats_text_total, fontsize=10, transform=axes[1].transAxes,
        verticalalignment='top', horizontalalignment='right',
        bbox=dict(facecolor='white', alpha=0.8, edgecolor='black')
    )
    axes[1].set_title('Histogram of Total Points')
    axes[1].set_xlabel('Total Points')
    axes[1].set_ylabel('Frequency')
    axes[1].grid(True)

    # Adjust layout and display the plots
    plt.tight_layout()
    plt.show()

# Example usage:
file_path = '/home/manos/MultiAgentSystems/app1/totalPoints.csv'
plot_histograms_with_stats_and_total(file_path)
