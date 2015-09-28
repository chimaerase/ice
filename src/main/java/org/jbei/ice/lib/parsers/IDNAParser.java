package org.jbei.ice.lib.parsers;

import org.jbei.ice.lib.vo.DNASequence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An object that parser sequences and generates an annotated {@link DNASequence} object.
 *
 * @author Zinovii Dmytriv, Timothy Ham
 */
public interface IDNAParser {
    /**
     * Return the name of the parser.
     *
     * @return Name of parser.
     */
    String getName();

    /**
     * Parse the given bytes to {@link DNASequence} annotated sequence.
     *
     * @param bytes
     * @return Annotated sequence.
     * @throws InvalidFormatParserException
     */
    DNASequence parse(byte[] bytes) throws InvalidFormatParserException;

    /**
     * Parse the given file to {@link DNASequence} annotated sequence.
     *
     * @param file File to parse.
     * @return Annotated sequence.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws InvalidFormatParserException
     */
    DNASequence parse(File file) throws IOException, InvalidFormatParserException;

    /**
     * Parse the given string to {@link DNASequence} annotated sequence.
     *
     * @param textSequence
     * @return parsed DNASequence.
     * @throws InvalidFormatParserException
     */
    DNASequence parse(String textSequence) throws InvalidFormatParserException;
}
