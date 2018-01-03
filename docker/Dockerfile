FROM openjdk:8-jre

ADD extract-identity-service-latest.war .

# Set locales for image to support UTF-8
ENV LANG='en_US.UTF-8' LANGUAGE='en_US:en' LC_ALL='en_US.UTF-8'

EXPOSE 8080

CMD ["java", "-jar", "extract-identity-service-latest.war"]