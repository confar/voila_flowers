from fastapi import FastAPI, File

from classifier import Classifier

resnet = Classifier()
app = FastAPI()


@app.get('/')
def healthcheck():
    return {'Hello': 'World'}


@app.post('/files/')
async def recognize_flower(file: bytes = File(...)):
    pred, pred_idx, probs = resnet.learner.predict(file)
    return {'prediction': pred,
            'probability': float(probs[pred_idx])}
