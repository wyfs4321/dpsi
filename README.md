# Dpsi

This is the source code of `Dpsi`.

## Testing with the Source Code

To compile and run `Dpsi`, you need to install the library [`mpc4j`](https://github.com/alibaba-edu/mpc4j) (v1.0.9) into your local Maven repository with the following step:

1. Clone mpc4j: `git clone https://github.com/alibaba-edu/mpc4j.git`.
2. Follow the guideline in `mpc4j` to install `mpc4j-native-fourq` and compile `mpc4j-native-tool`. Then, you need to assign the native library location using `-Djava.library.path`.
3. Goto the root path of mpc4j: `cd mpc4j`.
4. Package and install: `mvn install`.

Then, you can use an IDE (e.g., IntelliJ IDEA) to import the source code and run unit tests or generate the independent `jar` file for experiments. 


## Testing with the jar

You can generate the `jar` file by running `mvn package` in the root path of the source code. To run the project using the JAR file, execute the following command:

`java -Djol.magicFieldOffset=true -Djava.library.path=YOUR_MPC4J_COMMON_TOOL_LIB_PATH -jar pai_pir-1.0-SNAPSHOT-jar-with-dependencies.jar CONFIG_FILE.conf`

The example config files are shown in the dictionary `config/`.
