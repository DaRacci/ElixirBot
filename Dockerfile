FROM amazoncorretto:18.0.1-alpine3.15

RUN mkdir /bot
RUN mkdir /data

COPY build/libs/ElixirBot-*-all.jar /usr/local/lib/ElixirBot.jar

WORKDIR /bot

ENTRYPOINT ["java", "-Xms2G", "-Xmx2G", "-jar", "/usr/local/lib/ElixirBot.jar"]
