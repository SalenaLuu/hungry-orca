package com.codingseahorse.hungryorca.service;

import com.codingseahorse.hungryorca.exception.MyFileSaveException;
import com.codingseahorse.hungryorca.exception.NotFoundException;
import com.codingseahorse.hungryorca.model.OrcaFile;
import com.codingseahorse.hungryorca.repository.OrcaFileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OrcaFileService {

    @Autowired
    OrcaFileRepository orcaFileRepository;

    public void saveFile(MultipartFile multipartFile){
        String fileName =
                StringUtils.cleanPath(
                        Objects.requireNonNull(multipartFile.getOriginalFilename()));
        Pattern myPattern = Pattern.compile("[^A-Za-z0-9._]");
        Matcher m = myPattern.matcher(fileName);
        boolean existsSpecialSign = m.find();

        if(existsSpecialSign){
            throw new MyFileSaveException(
                    "Filename contains special characters. " +
                    "Please enter a valid fileName (a-z,A-Z,0-9)");
        }

        boolean orcaFileAlreadyExists = orcaFileRepository.existsOrcaFileByOrcaFileName(fileName);

        if (orcaFileAlreadyExists){
            throw new MyFileSaveException(
                    String.format("File with the name %s already exists",
                    fileName));
        }else {
            try {
                if(!fileName.contains(".")){
                    throw new MyFileSaveException(
                            "This is not a valid file format. " +
                            "The file should contain a '.' " +
                            "followed from a valid file-format like(.txt,.jpg,.png,...)");
                }
                OrcaFile orcaFile = new OrcaFile(
                        fileName,
                        multipartFile.getContentType(),
                        multipartFile.getBytes(),
                        new Date());
                orcaFileRepository.save(orcaFile);
            }catch (Exception e){
                throw new MyFileSaveException(
                        "A error has occurred. Failed saving file."
                        + Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public List<OrcaFile> retrieveAllOrcaFiles(){
        if(orcaFileRepository.findAll().isEmpty()){
            throw new NotFoundException("No OrcaFiles found in database. Please upload a file");
        }
        return orcaFileRepository.findAll();
    }


    public void download(String fileName, HttpServletResponse response) throws IOException {
        OrcaFile searchedOrcaFile = orcaFileRepository.findByOrcaFileName(fileName);

        if (searchedOrcaFile == null){
            throw new NotFoundException(String.format(
                    "file %s not found.",
                    fileName));
        }

        String headerValue =
                "attachment; filename=\"" +
                searchedOrcaFile.getOrcaFileName() + "\"";

        response.setContentType("application/octet-stream");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,headerValue);

        ServletOutputStream outputStream = response.getOutputStream();

        outputStream.write(searchedOrcaFile.getOrcaFileData());
        outputStream.close();
    }
}
