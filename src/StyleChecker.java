import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;

public class StyleChecker {

    private static String INPUT_FILE_NAME = null;
    private static String OUTPUT_FILE_NAME = null;
    private static int totalErrors = 0;
    private static int numIllFormedLabel = 0;
    private static int numOpCodeErrors = 0;
    private static int numTooFewOperands = 0;
    private static int numTooManyOperands = 0;
    private static int numIllFormedOperands = 0;
    private static int numWrongOperandType = 0;
    private static int numLabelProblems = 0;
    private static int numLabelWarnings = 0;
    private static ArrayList<String> data = new ArrayList<>();
    private static ArrayList<String> errors = new ArrayList<>();
    private static ArrayList<String> warnings = new ArrayList<>();

    /**
     * This is the main method. This will get the input file name from the user, assign the filename to a constant
     * variable, create the output filename then call the methods to complete the compiling.
     * @param args generic catch all for variables taken upon program launch.
     */
    public static void main(String[] args) {
        Scanner keyboard = new Scanner(System.in);
        System.out.println("Enter the filename of the file to check");

        INPUT_FILE_NAME = keyboard.nextLine();
        keyboard.close();

        OUTPUT_FILE_NAME = INPUT_FILE_NAME.substring(0, INPUT_FILE_NAME.lastIndexOf('.')) + ".log";

        readInFile();
        checkForCompileErrors();
        generateReport();
    }

    /**
     * This method is responsible for reading in the input file and putting each line into an arraylist
     * for further processing.
     */
    private static void readInFile() {
        try {
            Scanner file = new Scanner(new File(INPUT_FILE_NAME));

            while (file.hasNext()) {
                data.add(file.nextLine());
            }

            file.close();
        } catch (FileNotFoundException ex) {
            System.err.println("\nERROR: The input file name " + INPUT_FILE_NAME + " was not found.");
            System.err.println("Please check your input file and try again.");

            System.exit(0);
        }
    }

    /**
     * This method is responsible for all of the heavy lifting. It will run through every line of code read
     * in by the readInFile method and check for the
     */
    private static void checkForCompileErrors() {
        ArrayList<String> validOpCodes = new ArrayList<>(Arrays.asList("DEF", "COPY", "MOVE", "ADD", "INC", "SUB",
                                                                        "DEC", "MUL", "DIV", "BEQ", "BGT", "BR"));
        ArrayList<String> validRegisters = new ArrayList<>(Arrays.asList("R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7"));
        ArrayList<String> validLabels = new ArrayList<>();
        ArrayList<String> accessedLabels = new ArrayList<>();
        ArrayList<String> validMemoryLocations = new ArrayList<>();
        String label, opCode, firstOperand, secondOperand, thirdOperand;
        label = opCode = firstOperand = secondOperand = thirdOperand = null;

        /*
         * Retrieve all labels used
         */
        for (String line: data) {
            if (line.contains(":")) {
                String[] labelToken = line.split(":");
                validLabels.add(labelToken[0]);
            }
        }

        forEachChecker:
        for (String line: data) {
            label = opCode = firstOperand = secondOperand = thirdOperand = null;
            String[] token = new String[0];

            /*
             * Empty line
             */
            if (line.isEmpty()) {  // if line is blank
                continue forEachChecker;
            } else if (line.matches("\\s+")) {  //if line contains only spaces
                continue forEachChecker;
            }

            /*
             * Comments
             */
            if (line.startsWith(";")) {  //if line only contains comment
                if (!line.endsWith("ELO")) {  //check that line has end line operator
                    errors.add(line + "\n   **End line operator is required following a comment.");
                    totalErrors++;
                    numTooFewOperands++;
                    continue forEachChecker;
                } else {
                    continue forEachChecker;
                }
            } else if (line.contains(";")) {  //if line has comment at end
                if (!line.endsWith("ELO")) {  //check that line has end line operator
                    errors.add(line + "\n   **End line operator is required following a comment.");
                    totalErrors++;
                    numTooFewOperands++;
                    continue forEachChecker;
                } else {  //split line into operation and comment
                    String[] temp = line.split(";");
                    line = temp[0];

                    if (temp[0].matches("\\s+")) {  //if the text before the comment is only spaces
                        continue forEachChecker;
                    }
                }
            }

            /*
             * Seperate parameters
             */
            if (line.contains(":")) {
                String[] labelToken = line.split(":");
                token = labelToken[1].split(",|\\s+");
                label = labelToken[0];
            } else {
                label = null;
                token = line.split(",|\\s+");
            }

            if (token.length == 2) {
                opCode = token[1];
            } else if (token.length == 3) {
                opCode = token[1];
                firstOperand = token[2];
            } else if (token.length == 5) {
                opCode = token[1];
                firstOperand = token[2];
                secondOperand = token[4];
            } else if (token.length == 7) {
                opCode = token[1];
                firstOperand = token[2];
                secondOperand = token[4];
                thirdOperand = token[6];
            }

            /*
             * Ill Formed Label
             */
            if (label != null) {
                if (label.length() > 5) {
                    errors.add(line + "\n   **Ill Formed Label. Limit of 5 characters.");
                    totalErrors++;
                    numIllFormedLabel++;
                    continue forEachChecker;
                } else if (label.matches(".*\\d+.*")) {
                    errors.add(line + "\n   **Ill Formed Label. Label cannot contain numbers.");
                    totalErrors++;
                    numIllFormedLabel++;
                    continue forEachChecker;
                }
            }

            /*
             * Start and end command
             */
            if ("SRT".equals(opCode)) {
                errors.add(line);
                continue forEachChecker;
            }
            if ("END".equals(opCode)) {
                errors.add(line);
                break;
            }

            /*
             * Invalid Opcode
             */
            if (!validOpCodes.contains(opCode)) {
                errors.add(line + "\n   **Invalid Opcode. " + opCode + " was not found.");
                totalErrors++;
                numOpCodeErrors++;
                continue forEachChecker;
            }

            /*
             * Too many or too few operands
             */
            switch (opCode) {
                case "INC":
                case "DEC":
                case "BR":
                    if (firstOperand == null) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have a single operand.");
                        totalErrors++;
                        numTooFewOperands++;
                        continue forEachChecker;
                    } else if (secondOperand != null | thirdOperand != null) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have a single operand.");
                        totalErrors++;
                        numTooManyOperands++;
                        continue forEachChecker;
                    } else {
                        break;
                    }
                case "DEF":
                case "MOVE":
                case "COPY":
                    if (firstOperand == null | secondOperand == null) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have exactly 2 operands.");
                        totalErrors++;
                        numTooFewOperands++;
                        continue forEachChecker;
                    } else if (thirdOperand != null) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have exactly 2 operands.");
                        totalErrors++;
                        numTooManyOperands++;
                        continue forEachChecker;
                    } else {
                        break;
                    }
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                case "BGT":
                case "BEQ":
                    if (firstOperand == null | secondOperand == null | thirdOperand == null) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have exactly 3 operands.");
                        totalErrors++;
                        numTooFewOperands++;
                        continue forEachChecker;
                    } else if (token.length > 8) {
                        errors.add(line + "\n   **Invalid number of operands. This command should have exactly 3 operands.");
                        totalErrors++;
                        numTooManyOperands++;
                        continue forEachChecker;
                    } else {
                        break;
                    }
            }

            /*
             * Ill Formed Operand and wrong operand type
             */
            switch (opCode) {
                case "BR":
                   if (!validLabels.contains(firstOperand)) {
                        if (validRegisters.contains(firstOperand)) {
                            errors.add(line + "\n   **Wrong Operand Type." + firstOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Label Error. " + firstOperand + " is not a recognized label.");
                            totalErrors++;
                            numLabelProblems++;
                            continue forEachChecker;
                        }
                    } else {
                        accessedLabels.add(firstOperand);
                        break;
                    }
                case "INC":
                case "DEC":
                    if (!validRegisters.contains(firstOperand) & !validMemoryLocations.contains(firstOperand)) {
                        if (firstOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + firstOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                            continue forEachChecker;
                        }
                    } else {
                        break;
                    }
                case "DEF":
                    if (firstOperand.length() > 5 & firstOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " does not fit the rules for a memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validRegisters.contains(secondOperand)) {
                        if (secondOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + secondOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + secondOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                            continue forEachChecker;
                        }
                    } else {
                        validMemoryLocations.add(firstOperand);
                        break;
                    }
                case "COPY":
                    if (!validRegisters.contains(firstOperand) & !validMemoryLocations.contains(firstOperand)) {
                        if (firstOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + firstOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                            continue forEachChecker;
                        }
                    } else if (!validRegisters.contains(secondOperand) & !validMemoryLocations.contains(secondOperand)) {
                        if (secondOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + secondOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + secondOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                            continue forEachChecker;
                        }
                    } else {
                        break;
                    }
                case "MOVE":
                    if (!validRegisters.contains(firstOperand) & !validMemoryLocations.contains(firstOperand) & !firstOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " is not a number or recognized memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validRegisters.contains(secondOperand) & !validMemoryLocations.contains(secondOperand)) {
                        if (secondOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + secondOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + secondOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                        }
                        continue forEachChecker;
                    } else {
                        break;
                    }
                case "ADD":
                case "SUB":
                case "MUL":
                case "DIV":
                    if (!validRegisters.contains(firstOperand) & !firstOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " is not a number or recognized memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validRegisters.contains(secondOperand) & !secondOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + secondOperand + " is not a number or recognized memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validRegisters.contains(thirdOperand) & !validMemoryLocations.contains(thirdOperand)) {
                        if (thirdOperand.matches(".*\\d+.*")) {
                            errors.add(line + "\n   **Wrong Operand Type." + thirdOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Ill Formed Operand. " + thirdOperand + " is not a recognized memory location.");
                            totalErrors++;
                            numIllFormedOperands++;
                            continue forEachChecker;
                        }
                    } else {
                        break;
                    }
                case "BGT":
                case "BEQ":
                    if (!validRegisters.contains(firstOperand) & !validMemoryLocations.contains(firstOperand) & !firstOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + firstOperand + " is not a number or recognized memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validRegisters.contains(secondOperand) & !validMemoryLocations.contains(secondOperand) & !secondOperand.matches(".*\\d+.*")) {
                        errors.add(line + "\n   **Ill Formed Operand. " + secondOperand + " is not a number or recognized memory location.");
                        totalErrors++;
                        numIllFormedOperands++;
                        continue forEachChecker;
                    } else if (!validLabels.contains(thirdOperand)) {
                        if (validRegisters.contains(thirdOperand)) {
                            errors.add(line + "\n   **Wrong Operand Type." + thirdOperand + " should be a memory location.");
                            totalErrors++;
                            numWrongOperandType++;
                            continue forEachChecker;
                        } else {
                            errors.add(line + "\n   **Label Error. " + thirdOperand + " is not a recognized label.");
                            totalErrors++;
                            numLabelProblems++;
                            continue forEachChecker;
                        }
                    } else {
                        accessedLabels.add(thirdOperand);
                        break;
                    }
            }

            errors.add(line);
        }

        /*
         * Label Problem
         */
        for (String value: accessedLabels) {
            if (validLabels.contains(value)) {
                validLabels.remove(value);
            }
        }

        if (!validLabels.isEmpty()) {
            for (String value: validLabels) {
                numLabelWarnings++;
                warnings.add(value + " is never accessed.");
            }
        }
    }

    private static void generateReport() {
        PrintWriter output = null;

        final String heading = "PAL Compiler";
        final String myName = "Matt Kline";
        final String todaysDate = new SimpleDateFormat("MM-dd-yyyy").format(new Date());
        final String className = "CS 3210";
        final String readFrom = "Program was read from " + INPUT_FILE_NAME;

        try {
            output = new PrintWriter(new File(OUTPUT_FILE_NAME));
        } catch (FileNotFoundException ex) {
            System.err.println("Error: File " + OUTPUT_FILE_NAME + " was not found.");

            System.exit(0);
        }

        output.println(heading + "\n" + myName + "\n" + todaysDate + "\n" + className + "\n" + "\n" + readFrom);

        for (int i = 0; i < errors.size(); i++) {
            int lineNumber = i + 1;
            output.println(lineNumber + ". " + errors.get(i));
        }

        output.println("\nSummary" + "\n" + "--------------------------------------------------");
        output.println("Total Errors: " + totalErrors + ".");
        if (numIllFormedLabel > 0)
            output.println("    " + numIllFormedLabel + " Ill Formed Label Errors.");
        if (numOpCodeErrors > 0)
            output.println("    " + numOpCodeErrors + " Invalid Opcode Errors.");
        if (numTooFewOperands > 0)
            output.println("    " + numTooFewOperands + " Too Few Operands Errors.");
        if (numTooManyOperands > 0)
            output.println("    " + numTooManyOperands + " Too Many Operands Errors.");
        if (numIllFormedOperands > 0)
            output.println("    " + numIllFormedOperands + " Ill Formed Operands Errors.");
        if (numWrongOperandType > 0)
            output.println("    " + numWrongOperandType + " Wrong Operand Type Errors.");
        if (numLabelProblems > 0)
            output.println("    " + numLabelProblems + " Label Problem Errors.");

        output.println("");
        output.println("Total warnings: " + numLabelWarnings + ".");
        if (numLabelWarnings > 0)
            for (String warning: warnings)
                output.println("    " + warning);
        if (totalErrors > 0)
            output.println("\nPAL Compile completed with errors.");
        else
            output.println("\nPAL Compile completed successfully.");

        output.flush();
        output.close();
    }
}
