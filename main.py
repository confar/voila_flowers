from fastapi import FastAPI, File
from pydantic import BaseModel

from classifier import Classifier

resnet = Classifier()
app = FastAPI()


class PredictionResult(BaseModel):
    prediction: str
    probability: float


@app.get('/')
def healthcheck():
    return {'Hello': 'World'}


@app.post('/files/')
async def recognize_flower(file: bytes = File(...)):
    pred, pred_idx, probs = resnet.learner.predict(file)
    return PredictionResult(prediction=pred, probability=probs[pred_idx])
