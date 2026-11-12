FROM node:22-alpine

ENV NODE_ENV=production \
    WAYPOINT_DATA_DIR=/data

WORKDIR /app
COPY package.json package-lock.json ./
RUN apk add --no-cache su-exec \
    && npm ci --omit=dev \
    && mkdir -p /data
COPY --chown=node:node src ./src
COPY docker-entrypoint.sh ./

VOLUME /data
ENTRYPOINT ["sh", "./docker-entrypoint.sh"]
CMD ["npm", "start"]
