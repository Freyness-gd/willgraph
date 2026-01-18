FROM python:3.11-slim

ENV PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=on \
    PYTHONDONTWRITEBYTECODE=1 \
    DISPLAY=:99

WORKDIR /app

# System dependencies for Scrapy + Playwright/Chromium + visible UI (Xvfb + noVNC)
RUN apt-get update \
 && apt-get install -y --no-install-recommends \
    git curl wget ca-certificates \
    # Playwright/Chromium runtime deps (you already had these)
    libnss3 libatk1.0-0 libatk-bridge2.0-0 libdrm2 libxkbcommon0 libxrandr2 \
    libgbm1 libgtk-3-0 libasound2 libxcomposite1 libxdamage1 libxfixes3 \
    libpango-1.0-0 libpangocairo-1.0-0 libwoff1 \
    # Visible browser stack
    xvfb fluxbox x11vnc novnc websockify \
 && rm -rf /var/lib/apt/lists/*

COPY requirements.txt ./

# Install Python deps + Playwright, then Chromium
RUN pip install --no-cache-dir -r requirements.txt playwright \
 && playwright install --with-deps chromium

COPY . ./

EXPOSE 6080 5900

# Start virtual display + VNC + noVNC, then run your app
CMD ["bash", "-lc", "\
  Xvfb :99 -screen 0 1920x1080x24 -ac +extension GLX +render -noreset & \
  fluxbox & \
  x11vnc -display :99 -forever -shared -nopw -rfbport 5900 & \
  websockify --web=/usr/share/novnc/ 6080 localhost:5900 & \
  exec python scrapy/main.py --spiders all \
"]