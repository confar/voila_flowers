from fastapi import FastAPI, File

from classifier import Classifier

app = FastAPI()
app.model = Classifier()


@app.get('/')
def healthcheck():
    return {'alive': True}


@app.post('/files/')
async def recognize_flower(file: bytes = File(...)):
    pred, pred_idx, probs = app.model.learner.predict(file)
    return {'prediction': pred,
            'probability': float(probs[pred_idx])}
