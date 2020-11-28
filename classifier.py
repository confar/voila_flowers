import importlib
import json
from pathlib import Path

from fastai.data.transforms import parent_label
from fastai.learner import distrib_barrier, torch

pickle = importlib.import_module('pickle')


class CustomUnpickler(pickle.Unpickler):

    def find_class(self, module, name):
        try:
            return super().find_class(__name__, name)
        except AttributeError:
            return super().find_class(module, name)


pickle.Unpickler = CustomUnpickler


folder = Path('./')

with open(folder / 'category_to_name.json') as json_file:
    mapping = json.load(json_file)


def parent_label_to_name(o):
    """Label `item` with the parent folder name."""
    return mapping[parent_label(o)]


def load_learner(fname, cpu=True):
    """Load a `Learner` object in `fname`, optionally putting it on the `cpu`"""
    distrib_barrier()
    res = torch.load(fname, map_location='cpu' if cpu else None, pickle_module=pickle)
    if hasattr(res, 'to_fp32'):
        res = res.to_fp32()
    if cpu:
        res.dls.cpu()
    return res


class Singleton(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super(Singleton, cls).__call__(*args, **kwargs)
        else:
            cls._instances[cls].__init__(*args, **kwargs)
        return cls._instances[cls]


class Classifier(metaclass=Singleton):
    def __init__(self):
        self.learner = load_learner('export.pkl')
