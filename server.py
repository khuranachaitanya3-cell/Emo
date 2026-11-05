import os
from fastapi import FastAPI
from pydantic import BaseModel
from urllib.request import Request, urlopen
import json

app = FastAPI(title="EmoBuddy AI backend")

class ChatInput(BaseModel):
    message: str

@app.post("/chat")
def chat(data: ChatInput):
    key = os.environ.get("OPENAI_API_KEY")
    if not key:
        return {"reply": "Your backend is running, but its OPENAI_API_KEY is not configured yet."}
    payload = {
        "model": os.environ.get("OPENAI_MODEL", "gpt-4o-mini"),
        "messages": [
            {"role": "system", "content": "You are EmoBuddy, a cheerful, kind, concise robot companion. Reply naturally and warmly. The user may speak Hindi or English; respond in the same language."},
            {"role": "user", "content": data.message}
        ]
    }
    req = Request("https://api.openai.com/v1/chat/completions",
                  data=json.dumps(payload).encode(),
                  headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"})
    with urlopen(req, timeout=25) as response:
        result = json.loads(response.read())
    return {"reply": result["choices"][0]["message"]["content"]}
