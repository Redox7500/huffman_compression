import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main
{
    public static void main(String[] args)
    {
        Huffman.testEncodeDecodeFile(Paths.get("input.txt"), Paths.get("compressed.bin"), 2);
    }
}

class FileFunctions
{
    public static void writeBinary(String binary, Path path)
    {
        try
        {
            byte[] bytes = new byte[Math.ceilDiv(binary.length(), 8)];
            for (int i = 0; i < bytes.length; i++)
            {
                String substring = binary.substring(i * 8, Math.min(binary.length(), (i + 1) * 8));
                bytes[i] = (byte)Integer.parseInt(String.format("%-8s", substring).replace(' ', '0'), 2);
            }

            Files.write(path, bytes);
        }
        catch (IOException error)
        {
            System.err.println("Error reading file at " + path.toAbsolutePath() + ": " + error.getMessage());
        }
    }

    public static String readBinary(Path path)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(path);
            StringBuilder binary = new StringBuilder();
            for (byte b:bytes)
            {
                binary.append(Integer.toBinaryString((b & 0xff) + 0x100).substring(1));
            }

            return binary.toString();
        }
        catch (IOException error)
        {
            System.err.println("Error reading file at " + path.toAbsolutePath() + ": " + error.getMessage());
           
            return "";
        }
    }

    public static String readText(Path path)
    {
        try
        {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (IOException error)
        {
            System.err.println("Error reading file at " + path.toAbsolutePath() + ": " + error.getMessage());
           
            return "";
        }
    }
}

class Node
{
    public int frequency = 0;
    public byte symbol = 0;
    public Node leftChild = null;
    public Node rightChild = null;
    public String encoding = "";

    public Node()
    {
       
    }

    public Node(int frequency)
    {
        this.frequency = frequency;
    }

    public Node(int frequency, byte symbol)
    {
        this.frequency = frequency;
        this.symbol = symbol;
    }

    public Node(int frequency, Node leftChild, Node rightChild)
    {
        this.frequency = frequency;
        this.leftChild = leftChild;
        this.rightChild = rightChild;
    }

    public Map<Byte, String> getEncodings()
    {
        Map<Byte, String> encodings = new HashMap<>();

        List<Node> nodes = new ArrayList<>(List.of(this));
        while (!nodes.isEmpty())
        {
            Node node = nodes.get(0);
           
            if (node.leftChild == null)
            {
                encodings.put(node.symbol, node.encoding);
            }
            else
            {
                node.leftChild.encoding = node.encoding + "0";
                node.rightChild.encoding = node.encoding + "1";

                nodes.add(node.leftChild);
                nodes.add(node.rightChild);
            }

            nodes.remove(0);
        }

        return encodings;
    }

    public void insertSorted(List<Node> nodes)
    {
        int low = 0;
        int high = nodes.size() - 1;
        while (low <= high)
        {
            int mid = (low + (high - low) / 2);
            int midFrequency = nodes.get(mid).frequency;
   
            if (this.frequency < midFrequency)
            {
                high = mid - 1;
            }
            else if (this.frequency > midFrequency)
            {
                low = mid + 1;
            }
            else
            {
                nodes.add(mid, this);
                return;
            }
        }

        nodes.add(low, this);
    }
}

class Huffman
{
    private static List<Node> createLeaves(String text)
    {
        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        int[] frequencies = new int[256];
        for (int i = 0; i < textBytes.length; i++)
        {
            frequencies[textBytes[i] & 0xff]++;
        }

        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 256; i++)
        {
            int frequency = frequencies[i];
            if (frequencies[i] > 0)
            {
                nodes.add(new Node(frequency, (byte)(i)));
            }
        }

        return nodes;
    }

    private static Node combineLeaves(List<Node> nodes)
    {
        while (nodes.size() > 1)
        {
            Node node1 = nodes.get(0);
            Node node2 = nodes.get(1);
            Node newNode = new Node(node1.frequency + node2.frequency, node1, node2);

            newNode.insertSorted(nodes);
            nodes = nodes.subList(2, nodes.size());
        }

        return nodes.get(0);
    }

    public static String encode(String text)
    {
        if (text.length() == 0)
        {
            return "";
        }

        List<Node> leaves = createLeaves(text);
        Node rootNode = combineLeaves(leaves);
        Map<Byte, String> encodings = rootNode.getEncodings();
   
        StringBuilder binary = new StringBuilder();
        if (rootNode.leftChild == null)
        {
            binary.append('1');
            binary.append(Integer.toBinaryString((rootNode.symbol & 0xff) + 0x100).substring(1));
            binary.append(Integer.toBinaryString(rootNode.frequency));
        }
        else
        {
            List<Node> currentLayer = new ArrayList<>(List.of(rootNode));
            List<Node> nextLayer = new ArrayList<>();
            while (!currentLayer.isEmpty())
            {
                for (int i = 0; i < currentLayer.size(); i++)
                {
                    Node currentNode = currentLayer.get(i);
                    if (currentNode.leftChild == null)
                    {
                        binary.append('1');
                        binary.append(Integer.toBinaryString((currentNode.symbol & 0xff) + 0x100).substring(1));
                    }
                    else
                    {
                        binary.append('0');

                        nextLayer.add(currentNode.leftChild);
                        nextLayer.add(currentNode.rightChild);
                    }
                }

                currentLayer = new ArrayList<>(nextLayer);
                nextLayer.clear();
            }

            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            for (byte b:textBytes)
            {
                binary.append(encodings.get(b));
            }
        }
       
        int fullBinaryLength = binary.length() + 3;
        return String.format("%3s", Integer.toBinaryString(Math.ceilDiv(fullBinaryLength, 8) * 8 - fullBinaryLength)).replace(' ', '0') + binary.toString();
    }

    public static String decode(String binary)
    {
        if (binary.length() == 0)
        {
            return "";
        }
       
        binary = binary.substring(3, binary.length() - Integer.parseInt(binary.substring(0, 3), 2));
        if (binary.charAt(0) == '1')
        {
            int frequency = 0;
            for (int i = 9; i < binary.length(); i++)
            {
                frequency *= 2;
                frequency += binary.charAt(i) - '0';
            }

            byte[] textBytes = new byte[frequency];
            byte symbol = (byte)Integer.parseInt(binary.substring(1, 9), 2);
            Arrays.fill(textBytes, symbol);

            return new String(textBytes, StandardCharsets.UTF_8);
        }
        else
        {
            Node rootNode = new Node();

            List<Node> currentLayer = new ArrayList<>(List.of(rootNode));
            List<Node> nextLayer = new ArrayList<>();
            int bitIndex = 1;
            while (!currentLayer.isEmpty())
            {
                for (int currentNodeIndex = 0; currentNodeIndex < currentLayer.size() * 2; currentNodeIndex++)
                {
                    Node currentNode = currentLayer.get(currentNodeIndex / 2);
                    Node newNode = new Node();

                    if (binary.charAt(bitIndex) == '0')
                    {
                        nextLayer.add(newNode);
                    }
                    else
                    {
                        newNode.symbol = (byte)Integer.parseInt(binary.substring(bitIndex + 1, bitIndex + 9), 2);
                        bitIndex += 8;
                    }

                    if (currentNodeIndex % 2 == 0)
                    {
                        currentNode.leftChild = newNode;
                    }
                    else
                    {
                        currentNode.rightChild = newNode;
                    }

                    bitIndex++;
                }
           
                currentLayer = new ArrayList<>(nextLayer);
                nextLayer.clear();
            }
           
            List<Byte> textBytesList = new ArrayList<>();
   
            Node currentNode = rootNode;
            for (int i = bitIndex; i < binary.length(); i++)
            {
                if (currentNode.leftChild == null)
                {
                    textBytesList.add(currentNode.symbol);

                    currentNode = rootNode;
                }

                if (binary.charAt(i) == '0')
                {
                    currentNode = currentNode.leftChild;
                }
                else
                {
                    currentNode = currentNode.rightChild;
                }
            }
            if (currentNode.leftChild == null)
            {
                textBytesList.add(currentNode.symbol);
            }

            byte[] textBytes = new byte[textBytesList.size()];
            for (int i = 0; i < textBytesList.size(); i++)
            {
                textBytes[i] = textBytesList.get(i);
            }

            return new String(textBytes, StandardCharsets.UTF_8);
        }
    }
   
    public static void testEncodeDecodeFile(Path inputPath, Path outputPath, int logDetail)
    {
        String text = FileFunctions.readText(inputPath);
        if (logDetail >= 2)
        {
            System.out.println("original text: " + text + "\nread from: " + inputPath + "\n");            
        }

        String encoded = encode(text);
        if (logDetail >= 2)
        {
            System.out.println("encoded text: " + encoded);
        }
       
        FileFunctions.writeBinary(encoded, outputPath);
        if (logDetail >= 2)
        {
            System.out.println("wrote to: " + outputPath + "\n");
        }

        String binaryRead = FileFunctions.readBinary(outputPath);
        if (logDetail >= 2)
        {
            System.out.println("binary read: " + binaryRead);
            System.out.println("read from: " + outputPath + "\n");
        }

        String decoded = decode(binaryRead);
        if (logDetail >= 2)
        {
            System.out.println("decoded: " + decoded + "\n");
        }

        if (logDetail >= 1)
        {
            System.out.println(((text.equals(decoded))? "successful":"unsuccessful") + "\n");

            int naiveBitsUsed = text.getBytes(StandardCharsets.UTF_8).length * 8;
            System.out.println("bits used with naive encoding: " + naiveBitsUsed);
            System.out.println("bits used after compression: " + binaryRead.length());
            System.out.println("bits saved: " + (naiveBitsUsed - binaryRead.length()));
        }
    }
   
    public static void testEncodeDedcodeFile(Path inputPath, Path outputPath)
    {
        Huffman.testEncodeDecodeFile(inputPath, outputPath, 0);
    }
}