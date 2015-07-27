/*
 * This file is part of Fim - File Integrity Manager
 *
 * Copyright (C) 2015  Etienne Vrignaud
 *
 * Fim is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Fim is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fim.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fim.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;

import org.fim.model.FileHash;
import org.fim.model.HashMode;
import org.fim.tooling.BuildableParameters;
import org.fim.tooling.StateAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class FileHasherTest extends StateAssert
{
	public static final String NO_HASH = "no_hash";

	private StateGenerator stateGenerator;

	private HashMode hashMode;
	private BuildableParameters parameters;
	private Path rootDir;
	private FileHasher cut;

	public FileHasherTest(final HashMode hashMode)
	{
		this.hashMode = hashMode;
	}

	@Parameterized.Parameters(name = "Hash mode: {0}")
	public static Collection<Object[]> parameters()
	{
		return Arrays.asList(new Object[][]{
				{HashMode.DONT_HASH_FILES},
				{HashMode.HASH_ONLY_FIRST_FOUR_KILO},
				{HashMode.HASH_ONLY_FIRST_MEGA},
				{HashMode.COMPUTE_ALL_HASH}
		});
	}

	@Before
	public void setup() throws NoSuchAlgorithmException, IOException
	{
		stateGenerator = mock(StateGenerator.class);
		parameters = defaultParameters();
		parameters.setHashMode(hashMode);
		when(stateGenerator.getParameters()).thenReturn(parameters);

		rootDir = Paths.get("target/" + this.getClass().getSimpleName());
		Files.createDirectories(rootDir);

		cut = new FileHasher(stateGenerator, null, rootDir.toString());
	}

	@Test
	public void weCanConvertToHexa()
	{
		byte[] bytes = new byte[]{(byte) 0xa4, (byte) 0xb0, (byte) 0xe5, (byte) 0xfd};
		String hexString = cut.toHexString(bytes);
		assertThat(hexString).isEqualTo("a4b0e5fd");
	}

	@Test
	public void weCanConvertToHexaWithZero()
	{
		byte[] bytes = new byte[]{(byte) 0xa0, 0x40, 0x0b, 0x00, (byte) 0xe0, 0x05, 0x0f, 0x0d};
		String hexString = cut.toHexString(bytes);
		assertThat(hexString).isEqualTo("a0400b00e0050f0d");
	}

	@Test
	public void weCanGetTheRelativeFileName()
	{
		String relativeFileName = cut.getRelativeFileName("/dir1/dir2/dir3", "/dir1/dir2/dir3/dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir4/file1");

		relativeFileName = cut.getRelativeFileName("/dir5/dir6/dir7", "/dir1/dir2/dir3/dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir1/dir2/dir3/dir4/file1");

		relativeFileName = cut.getRelativeFileName("/dir1/dir2/dir3", "dir4/file1");
		assertThat(relativeFileName).isEqualTo("dir4/file1");
	}

	@Test
	public void weCanHashA_2KB_File() throws IOException
	{
		String firstFourKiloHash = "76b1e87b9f5df5d1584c5684432005d533196532439edbbf25ba9c7e82b7b0f7652c66e20ab07d854b950c8eeb5e2f65a03054f68d093fa75927ab2041bd8f74";
		String firstMegaHash = firstFourKiloHash;
		String fullFileHash = firstFourKiloHash;

		Path fileToHash = createFileWithSize(2 * 1024);

		FileHash fileHash = cut.hashFile(fileToHash, Files.size(fileToHash));

		assertFileHash(fileHash, firstFourKiloHash, firstMegaHash, fullFileHash);
	}

	@Test
	public void weCanHashA_30KB_File() throws IOException
	{
		String firstFourKiloHash = "757af34fe2d75e895caf4e479e77e5b2ba97510140933c89facc0399eb92063e83d7833d5d3285d35ee310b6d599aa8f8cafbd480cb797bbb2d8b8b47880d2ba";
		String firstMegaHash = "f66f942e45d12bda1224a7644e7b157a67e0cb66dc48e36d92cfbf8febf3fdae2d567a0906f1c3684f19e0902460513cf9f5fba285ce9d8f61fd1ea4772d79c3";
		String fullFileHash = firstMegaHash;

		Path fileToHash = createFileWithSize(30 * 1024);

		FileHash fileHash = cut.hashFile(fileToHash, Files.size(fileToHash));

		assertFileHash(fileHash, firstFourKiloHash, firstMegaHash, fullFileHash);
	}

	@Test
	public void weCanHashA_60MB_File() throws IOException
	{
		String firstFourKiloHash = "757af34fe2d75e895caf4e479e77e5b2ba97510140933c89facc0399eb92063e83d7833d5d3285d35ee310b6d599aa8f8cafbd480cb797bbb2d8b8b47880d2ba";
		String firstMegaHash = "733e3c1c2e1a71086637cecfe168a47d35c10cda2b792ff645befef7eaf86b96ecaf357b775dd323d5ab2a638c90c81abcae89372500dd8da60160508486bf4d";
		String fullFileHash = "e891a71e312bc6e34f549664706951516c42f660face62756bb155301c5e06ba79db94f83dedd43467530021935f5b427a58d7a5bd245ea1b2b0db8d7b08ee7a";

		Path fileToHash = createFileWithSize(60 * 1024 * 1024);

		FileHash fileHash = cut.hashFile(fileToHash, Files.size(fileToHash));

		assertFileHash(fileHash, firstFourKiloHash, firstMegaHash, fullFileHash);
	}

	private void assertFileHash(FileHash fileHash, String firstFourKiloHash, String firstMegaHash, String fullFileHash)
	{
		switch (hashMode)
		{
			case DONT_HASH_FILES:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case HASH_ONLY_FIRST_FOUR_KILO:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(firstFourKiloHash);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case HASH_ONLY_FIRST_MEGA:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(NO_HASH);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(firstMegaHash);
				assertThat(fileHash.getFullHash()).isEqualTo(NO_HASH);
				break;

			case COMPUTE_ALL_HASH:
				assertThat(fileHash.getFirstFourKiloHash()).isEqualTo(firstFourKiloHash);
				assertThat(fileHash.getFirstMegaHash()).isEqualTo(firstMegaHash);
				assertThat(fileHash.getFullHash()).isEqualTo(fullFileHash);
				break;
		}
	}

	private Path createFileWithSize(long fileSize) throws IOException
	{
		Path license = Paths.get("LICENSE");
		byte[] content = Files.readAllBytes(license);

		Path newFile = rootDir.resolve("LICENSE_" + fileSize);
		if (Files.exists(newFile))
		{
			Files.delete(newFile);
		}

		if (content.length > fileSize)
		{
			content = Arrays.copyOf(content, (int) fileSize);
		}

		for (int size = 0; size < fileSize; size += content.length)
		{
			Files.write(newFile, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		}

		return newFile;
	}
}