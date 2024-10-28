from tqdm import tqdm
import numpy as np

from java_api import Autocomplete, Compile

TEMPERATURE = 0.5
TOPK = 20
SEED = 0

print(f"Running with seed {SEED} and temperature {TEMPERATURE} and topk {TOPK}")

compiler = Compile()
auto = Autocomplete()

temp_string = str(TEMPERATURE).replace(".", "dot")
train_samples, train_is_duplicate = np.load(f"./baselines/train_sample_outputs/train_samples_temp={temp_string}_k={TOPK}_seed={SEED}.npy", allow_pickle=True)
val_samples, val_is_duplicate = np.load(f"./baselines/train_sample_outputs/val_samples_temp={temp_string}_k={TOPK}_seed={SEED}.npy", allow_pickle=True)

train_compiles = []
for sample in tqdm(train_samples, desc="Compiling training samples", total=len(train_samples), leave=False):
    try:
        next_tokens = auto.next_tokens(sample)
        compilable = (next_tokens == ['COMPLETE!'])
    except:
        compilable = False
        auto = Autocomplete()

    train_compiles.append(compilable)


novel_and_compilable = [(not (duplicate == "True")) and compilable for duplicate, compilable in zip(train_is_duplicate, train_compiles)]
print(f"Proportion of novel training samples: {np.mean([not (duplicate == 'True') for duplicate in train_is_duplicate])}")
print(f"Proportion of compilable training samples: {np.mean(train_compiles)}")
print(f"Proportion of novel and compilable training samples: {np.mean(novel_and_compilable)}")

val_compiles = []
for sample in tqdm(val_samples, desc="Compiling validation samples", total=len(val_samples), leave=False):
    try:
        next_tokens = auto.next_tokens(sample)
        compilable = (next_tokens == ['COMPLETE!'])
    except:
        compilable = False
        auto = Autocomplete()

    val_compiles.append(compilable)

novel_and_compilable = [(not (duplicate == "True")) and compilable for duplicate, compilable in zip(val_is_duplicate, val_compiles)]
print(f"\nProportion of novel validation samples: {np.mean([not (duplicate == 'True') for duplicate in val_is_duplicate])}")
print(f"Proportion of compilable validation samples: {np.mean(val_compiles)}")
print(f"Proportion of novel and compilable validation samples: {np.mean(novel_and_compilable)}")