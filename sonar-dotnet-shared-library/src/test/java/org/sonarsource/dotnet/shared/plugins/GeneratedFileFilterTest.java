/*
 * SonarSource :: .NET :: Shared library
 * Copyright (C) 2014-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.dotnet.shared.plugins;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.utils.log.LogTester;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonarsource.dotnet.protobuf.SonarAnalyzer.FileMetadataInfo;

import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeneratedFileFilterTest {
  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Rule
  public LogTester logs = new LogTester();

  private AbstractConfiguration configuration;

  @Test
  public void accept_returns_false_for_autogenerated_files() throws IOException {
    // Arrange
    GeneratedFileFilter filter = createFilter(Paths.get("autogenerated"));

    // Act
    Boolean result = filter.accept(newInputFile("autogenerated"));

    // Assert
    assertThat(result).isFalse();
    assertThat(logs.logs(LoggerLevel.DEBUG)).contains("Skipping auto generated file: autogenerated");
  }

  @Test
  public void accept_returns_true_for_nonautogenerated_files() throws IOException {
    // Arrange
    GeneratedFileFilter filter = createFilter(Paths.get("c:\\autogenerated"));

    // Act
    Boolean result = filter.accept(newInputFile("File1"));

    // Assert
    assertThat(result).isTrue();
    assertThat(logs.logs(LoggerLevel.DEBUG)).isEmpty();
  }

  @Test
  public void is_initialized_only_once() throws IOException {
    // Arrange
    GeneratedFileFilter filter = createFilter();

    // Act - call accept several times and ensure we initialize the filter only once
    filter.accept(newInputFile("File1"));
    filter.accept(newInputFile("File2"));
    filter.accept(newInputFile("File3"));

    // Assert
    verify(configuration, times(1)).protobufReportPathsSilent();
  }

  private InputFile newInputFile(String path) {
    InputFile file = mock(InputFile.class);
    when(file.path()).thenReturn(Paths.get(path));
    when(file.toString()).thenReturn(path);
    return file;
  }

  private GeneratedFileFilter createFilter(Path... generated) throws IOException {
    File reportPath = temp.newFolder();
    try (OutputStream fos = Files.newOutputStream(reportPath.toPath().resolve("file-metadata.pb"))) {
      stream(generated).forEach(p -> {
        try {
          FileMetadataInfo.newBuilder().setFilePath(p.toString()).setIsGenerated(true).build().writeDelimitedTo(fos);
        } catch (IOException e) {
          fail(e.getMessage(), e);
        }
      });
    }
    configuration = mock(AbstractConfiguration.class);
    when(configuration.protobufReportPathsSilent()).thenReturn(Collections.singletonList(reportPath.toPath()));

    return new GeneratedFileFilter(configuration);
  }
}
