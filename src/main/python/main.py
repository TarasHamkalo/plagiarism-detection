import logging
import logging.config
import pathlib

import yaml

logger = logging.getLogger('MAIN')

def setup_logging():
  config_file = pathlib.Path("resources/logging.yaml")
  with open(config_file, 'r') as f:
    config = yaml.safe_load(f)

  logging.config.dictConfig(config)

if __name__ == "__main__":
  setup_logging()