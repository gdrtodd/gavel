# GAVEL: Games via Evolution and Language Models
This repository contains code and data for the GAVEL project, including a local fork of the Ludii source code and dataset.

The fine-tuned model checkpoint and dataset are avaiable [here](https://huggingface.co/LudiiLMs/code-llama-13b-fitm-mask-heldout-1-epoch) and [here](https://huggingface.co/datasets/LudiiLMs/code-llama-13b-fitm-mask-heldout-1-epoch-base-data), respectively.

The primary experiment can be run with the following command:
```
python evolution.py --model LudiiLMs/code-llama-13b-fitm-mask-heldout-1-epoch --fitness_evaluation_strategy uct \
    --games_per_eval 10 --num_fitness_evals 1 --thinking_time 0.25 --max_turns 50 --archive_type pca \
    --num_selections 3 --num_mutations 3 --fitness_eval_timeout 300 --num_threads 6 \
    --save_dir ./exp_outputs/main_experiment  --overwrite --add_current_date --spin_gpu \
    --seed 1
```

# Citation
If you use GAVEL in your work, please cite it as follows:
```
@inproceedings{todd2024gavel,
  title={GAVEL: Generating Games Via Evolution and Language Models},
  author={Todd, Graham and Padula, Alexander and Stephenson, Matthew and Piette, Eric and Soemers, Dennis and Togelius, Julian},
  booktitle={NeurIPS 2024, the Thirty-Eighth Annual Conference on Neural Information Processing Systems},
  year={2024}
}
```