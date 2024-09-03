# Use the OpenJDK image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the entire project to the container's working directory
COPY . /app

RUN apt-get update && apt-get install -y curl && apt-get clean

# Compile Java source files and include the external libraries
RUN javac -cp "lib/*" src/Main/Main.java src/Main/Java/*.java

# Run the compiled program, setting the classpath to include the libraries
CMD ["java", "-cp", "src:lib/*", "Main.Main"]
