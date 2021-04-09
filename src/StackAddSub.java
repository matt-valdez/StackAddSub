import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Stack;

/**
 * Author: Matthew Valdez
 * Date:   04/09/2021
 *
 * Reads a structured file that contains [number] [operand] [number]
 * and performs the math equation.
 */
public class StackAddSub {



    private final static int LEFT_INDEX = 0;
    private final static int RIGHT_INDEX = 2;
    private final static int OPERAND = 1;


    public StackAddSub() {
    }


    /**
     * Reads supplied FileName and processes each line
     * @param filename file to process
     */
    public void readFileAndProcess(String filename) {
        InputStream in = StackAddSub.class.getResourceAsStream(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));



        //this is where we loop over each line in the file
        while (true) {

            Stack<String> answer;
            String line = "";
            EquationData equationData = new EquationData();

            try {
                if ((line = reader.readLine()) == null) break;
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.printf("%s%n", line);

            String[] parts = line.split(" ");

            this.propagateStacks(parts, equationData);

            answer = this.performMath(equationData);


//            System.out.printf("Left:  %s\nOperand: %s\nRight: %s\nNegative Answer: %s\nNegative Left: %s\n" +
//                            "Negative Right: %s\n%n", Arrays.toString(equationData.getLeft().toArray()),
//                    equationData.getOperand(), Arrays.toString(equationData.getRight().toArray()),
//                    equationData.isNegativeAns(), equationData.isNegativeLeft(), equationData.isNegativeRight());


            // Read the stack of the answer
            StringBuilder sb = new StringBuilder();
            while (!answer.empty())
                sb.append(answer.pop());

            System.out.printf("= %s\n\n", sb.toString());
        }
    }

    /**
     * Reads the strings from the read in line and populates the queues
     * Also determines if the sides are negative
     * @param splitLine lines read in from file
     * @param equationData the equations data
     */
    private void propagateStacks(String[] splitLine, EquationData equationData) {
        int count = 0;
        String regex = "[^a-zA-Z0-9]";

        //Remove any "-" characters so we can get a length of the actual number
        String leftNumStr = splitLine[0].replaceAll(regex, "");
        String rightNumStr = splitLine[2].replaceAll(regex, "");

        for (String num : splitLine[StackAddSub.LEFT_INDEX].split("")) {

            try {
                int toPush = Integer.parseInt(num);

                if (count == 0) {
                    while (equationData.getLeft().size() < (rightNumStr.length() - leftNumStr.length())) {
                        equationData.getLeft().push(0);
                    }
                    //buffer 0
                    equationData.getLeft().push(0);
                }

                equationData.getLeft().push(toPush);
            } catch (NumberFormatException e) {
                //If this is the first time around. Assume it is negative number
                if (count == 0) {
                    equationData.setNegativeLeft(true);
                    count = -1;
                }
            }
            count++;
        }

        //set the operator
        equationData.setOperand(Operand.fromText(splitLine[StackAddSub.OPERAND]));

        count = 0;
        for (String num : splitLine[StackAddSub.RIGHT_INDEX].split("")) {
            try {
                int toPush = Integer.parseInt(num);

                if (count == 0) {
                    while (equationData.getRight().size() < (leftNumStr.length() - rightNumStr.length())) {
                        equationData.getRight().push(0);
                    }
                    //buffer 0
                    equationData.getRight().push(0);
                }

                equationData.getRight().push(toPush);
            } catch (NumberFormatException e) {
                //If this is the first time around. Assume it is negative number
                if (count == 0) {
                    equationData.setNegativeRight(true);
                    count = -1;
                }
            }
            count++;
        }

        determineLeftRight(leftNumStr, rightNumStr, equationData);
    }

    /**
     * Determines the left and right number in the equation and determines the operator
     * @param leftNumStr left number
     * @param rightNumStr right number
     * @param equationData equation data
     */
    private void determineLeftRight(String leftNumStr, String rightNumStr, EquationData equationData) {

        // If the right side is larger. Flip Right with Left.
        if (rightNumStr.length() > leftNumStr.length()) {
            Stack<Integer> tmp = equationData.getRight();
            equationData.setRight(equationData.getLeft());
            equationData.setLeft(tmp);

            boolean temp = equationData.isNegativeRight();
            equationData.setNegativeRight(equationData.isNegativeLeft());
            equationData.setNegativeLeft(temp);

            // If it was a subtraction, we need to make the right side negative and set operator to ADD
            if (equationData.getOperand() == Operand.SUB) {
                if(equationData.isNegativeLeft())
                    equationData.setNegativeLeft(false);
                else
                    equationData.setNegativeLeft(true);
                equationData.setOperand(Operand.ADD);
            }

        }

        // If left and right are negative. We will have negative answer
        if (equationData.isNegativeLeft() && equationData.isNegativeRight()) {
            equationData.setNegativeAns(true);

        // if left is negative. We will have negative answer. Left > Right
        } else if (equationData.isNegativeLeft()) {
            equationData.setNegativeAns(true);

            // If we are Adding we are Subtracting and if we are subtracting we are adding.
            if (equationData.getOperand() == Operand.ADD) {
                equationData.setOperand(Operand.SUB);
            }else{
                equationData.setOperand(Operand.ADD);
            }
        //do we have a negative right number?
        } else if (equationData.isNegativeRight()) {
            // double negative == addition else we are subtracting
            if (equationData.getOperand() == Operand.SUB) {
                equationData.setOperand(Operand.ADD);
            }else{
                equationData.setOperand(Operand.SUB);
            }
            equationData.setNegativeRight(false);
        }
    }

    /**
     * Perform math on equation data
     * @param equationData data for equation
     * @return answer
     */
    public Stack<String> performMath(EquationData equationData) {
        Stack<String> answer = new Stack<>();
        switch (equationData.operand) {
            case SUB:
                this.subtract(answer, equationData);
                break;
            case ADD:
                this.add(answer, equationData);
                break;
        }

        if (equationData.isNegativeAns())
            answer.push("-");

        return answer;
    }

    /**
     * Subtracts 2 stacks
     * @param answer of subtraction
     * @param equationData is the equation data
     */
    private void subtract(Stack<String> answer, EquationData equationData) {
        boolean remainder = false;
        while (!equationData.getLeft().empty()) {
            int leftNum = equationData.getLeft().pop();
            int rightNum = equationData.getRight().pop();
            int sub;

            //minus the borrowed 1
            if (remainder) {
                leftNum--;
                remainder = false;
            }

            //borrow 10
            if (leftNum < rightNum) {
                leftNum = leftNum + 10;
                remainder = true;
            }


            sub = leftNum - rightNum;

            answer.push(Integer.toString(sub));

        }

        while (answer.peek().equals("0"))
            answer.pop();
    }

    /**
     * summation of 2 stacks
     * @param answer of summation
     * @param equationData is the equation data
     */
    private void add( Stack<String> answer, EquationData equationData) {
        boolean remainder = false;

        while (!equationData.getLeft().empty()) {

            int leftNum = equationData.getLeft().pop();
            int rightNum = equationData.getRight().pop();
            int added;

            if (remainder) {
                leftNum++;
                remainder = false;
            }

            added = leftNum + rightNum;

            // Remainder logic
            if (added >= 10) {
                remainder = true;
                added = added - 10;
            }

            answer.push(Integer.toString(added));


        }
        //Removing the buffer 0
        while (answer.peek().equals("0"))
            answer.pop();

    }

    /**
     * Operand Enum
     */
    private enum Operand {
        ADD,
        SUB;

        public static Operand fromText(String text){
            if(text.equals("+"))
                return ADD;
            else if(text.equals("-"))
                return SUB;
            else
                return null;
        }
    }

    /**
     * Holder for Equation Data
     */
    private class EquationData{
        Stack<Integer> left;//left number
        Stack<Integer> right;//right number
        boolean negativeLeft;//is negative left
        boolean negativeRight;//is negative right
        boolean negativeAns;//will we have a negative answer
        Operand operand;//what math operator

        public EquationData(){
            left = new Stack<>();
            right = new Stack<>();
            negativeLeft = false;
            negativeRight = false;
            negativeAns = false;
        }

        public Stack<Integer> getLeft() {
            return left;
        }

        public void setLeft(Stack<Integer> left) {
            this.left = left;
        }

        public Stack<Integer> getRight() {
            return right;
        }

        public void setRight(Stack<Integer> right) {
            this.right = right;
        }

        public boolean isNegativeLeft() {
            return negativeLeft;
        }

        public void setNegativeLeft(boolean negativeLeft) {
            this.negativeLeft = negativeLeft;
        }

        public boolean isNegativeRight() {
            return negativeRight;
        }

        public void setNegativeRight(boolean negativeRight) {
            this.negativeRight = negativeRight;
        }

        public boolean isNegativeAns() {
            return negativeAns;
        }

        public void setNegativeAns(boolean negativeAns) {
            this.negativeAns = negativeAns;
        }

        public Operand getOperand() {
            return operand;
        }

        public void setOperand(Operand operand) {
            this.operand = operand;
        }
    }

    public static void main(String[] args) {

        StackAddSub stackAddSub = new StackAddSub();
        stackAddSub.readFileAndProcess("/addsAndSubtracts.txt");

    }
}
