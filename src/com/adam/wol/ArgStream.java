package com.adam.wol;

public class ArgStream {
    String[] args;
    int index = 0;

    public ArgStream(String[] s) {
        args = s;
    }

    public boolean hasNext() {
        return index < args.length;
    }

    public String getNext() {
        if (!hasNext())
            throw new IndexOutOfBoundsException();
        return args[index++];
    }

    public int getNextInt() throws NumberFormatException, IndexOutOfBoundsException {
        if (!hasNext())
            throw new IndexOutOfBoundsException();
        return Integer.parseInt(args[index++]);
    }

    public void printAll() {
        for (String s : args)
            System.out.println(s);
    }
}
