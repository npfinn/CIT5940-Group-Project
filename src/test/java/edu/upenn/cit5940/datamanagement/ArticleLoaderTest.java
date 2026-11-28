package edu.upenn.cit5940.datamanagement;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import edu.upenn.cit5940.common.dto.Article;

import edu.upenn.cit5940.logging.AppLogger;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;




class ArticleLoaderTest {
	
	//JUnit gives each test its own temporary folder
    @TempDir
    Path loadingFolder;

	//Both sample files should load the same article details
    @Test
    public void testSampleFilesMatch() throws IOException {

        ArticleLoader loader = new ArticleLoader();

        List<Article> csvArticles = loader.load("data/articles_small.csv");
        List<Article> jsonArticles = loader.load("data/articles_small.json");

        assertEquals(100, csvArticles.size());
        assertEquals(csvArticles.size(), jsonArticles.size());

        for (int articlePosition = 0; articlePosition < csvArticles.size(); articlePosition++) {

            Article csvArticle = csvArticles.get(articlePosition);
            Article jsonArticle = jsonArticles.get(articlePosition);

            assertEquals(csvArticle.getId(), jsonArticle.getId());
            assertEquals(csvArticle.getDate(), jsonArticle.getDate());
            assertEquals(csvArticle.getTitle(), jsonArticle.getTitle());
            assertEquals(csvArticle.getBody(), jsonArticle.getBody());
            assertEquals(csvArticle.getUrl(), jsonArticle.getUrl());
            assertEquals(csvArticle.getSource(), jsonArticle.getSource());
        }
    }
    //Quoted commas and line breaks should stay inside the article
    @Test
    public void testQuotedCsvFields() throws IOException {

        Path shipmentFile = loadingFolder.resolve("shipment_notes.csv");

        String shipmentRows = "uri,date,title,body,url,source\n";
        shipmentRows += "shipment-46,2022-11-09,\"Depot, shipping update\",\"Crates marked \"\"fragile\"\" arrived.\nInspection starts tomorrow.\",https://example.com/shipment,Depot Bulletin\n";

        Files.writeString(shipmentFile, shipmentRows);

        ArticleLoader loader = new ArticleLoader();
        List<Article> shipmentArticles = loader.load(shipmentFile.toString());

        assertEquals(1, shipmentArticles.size());

        Article shipmentArticle = shipmentArticles.get(0);

        assertEquals("shipment-46", shipmentArticle.getId());
        assertEquals(LocalDate.of(2022, 11, 9), shipmentArticle.getDate());
        assertEquals("Depot, shipping update", shipmentArticle.getTitle());
        assertEquals("Crates marked \"fragile\" arrived.\nInspection starts tomorrow.", shipmentArticle.getBody());
        assertEquals("https://example.com/shipment", shipmentArticle.getUrl());
        assertEquals("Depot Bulletin", shipmentArticle.getSource());
    }

    //Skip incomplete records and bad dates without losing later articles
    @Test
    public void testCsvSkipsBadRecords() throws IOException {

        Path repairFile = loadingFolder.resolve("repair_records.csv");

        String repairRows = "uri,date,title,body,url,source\n";
        repairRows += "repair-31,2022-11-04,Sensor repaired,Readings resumed,https://example.com/repair,Workshop Notes\n";
        repairRows += "repair-32,2022-02-30,Invalid date,Readings resumed,https://example.com/date,Workshop Notes\n";
        repairRows += "repair-33,2022-11-05,,Missing title,https://example.com/title,Workshop Notes\n";
        repairRows += "repair-34,2022-11-06\n";
        repairRows += "repair-35,2022-11-07,Sensor checked,Readings stable,https://example.com/check,Workshop Notes\n";

        Files.writeString(repairFile, repairRows);

        ArticleLoader loader = new ArticleLoader();
        List<Article> repairArticles = loader.load(repairFile.toString());

        assertEquals(2, repairArticles.size());
        assertEquals("repair-31", repairArticles.get(0).getId());
        assertEquals("repair-35", repairArticles.get(1).getId());
    }

    //JSON should also skip records with missing fields or bad dates
    @Test
    public void testJsonSkipsBadRecords() throws IOException {

        Path batteryFile = loadingFolder.resolve("battery_records.json");

        String batteryRecords = "[";
        batteryRecords += "{\"uri\":\"battery-71\",\"date\":\"2022-11-04\",\"title\":\"Battery inspected\",\"body\":\"Voltage remained stable\",\"url\":\"https://example.com/battery\",\"source\":\"Maintenance Notes\"},";
        batteryRecords += "{\"uri\":\"battery-72\",\"date\":\"2022-02-30\",\"title\":\"Bad date\",\"body\":\"Voltage recorded\",\"url\":\"https://example.com/date\",\"source\":\"Maintenance Notes\"},";
        batteryRecords += "{\"uri\":\"battery-73\",\"date\":\"2022-11-06\",\"title\":\"Missing body\",\"url\":\"https://example.com/body\",\"source\":\"Maintenance Notes\"},";
        batteryRecords += "{\"uri\":\"battery-74\",\"date\":\"2022-11-07\",\"title\":\"Battery replaced\",\"body\":\"Equipment restarted\",\"url\":\"https://example.com/replacement\",\"source\":\"Maintenance Notes\"}";
        batteryRecords += "]";

        Files.writeString(batteryFile, batteryRecords);

        ArticleLoader loader = new ArticleLoader();
        List<Article> batteryArticles = loader.load(batteryFile.toString());

        assertEquals(2, batteryArticles.size());
        assertEquals("battery-71", batteryArticles.get(0).getId());
        assertEquals("battery-74", batteryArticles.get(1).getId());
    }

    //Only the first record with an ID should survive loading
    @Test
    public void testLoaderKeepsFirstDuplicate() throws IOException {

        Path routeFile = loadingFolder.resolve("route_records.csv");

        String routeRows = "uri,date,title,body,url,source\n";
        routeRows += "route-58,2022-11-03,Original route,Truck leaves at noon,https://example.com/original,Route Bulletin\n";
        routeRows += "route-58,2022-11-08,Repeated route,Truck leaves at dusk,https://example.com/repeated,Route Bulletin\n";
        routeRows += "route-59,2022-11-09,Separate route,Truck leaves at dawn,https://example.com/separate,Route Bulletin\n";

        Files.writeString(routeFile, routeRows);

        ArticleLoader loader = new ArticleLoader();
        List<Article> routeArticles = loader.load(routeFile.toString());

        assertEquals(2, routeArticles.size());
        assertEquals("route-58", routeArticles.get(0).getId());
        assertEquals("Original route", routeArticles.get(0).getTitle());
        assertEquals(LocalDate.of(2022, 11, 3), routeArticles.get(0).getDate());
        assertEquals("route-59", routeArticles.get(1).getId());
    }

    //Valid files with no articles should return empty lists
    @Test
    public void testFilesWithoutArticles() throws IOException {

        Path blankCsv = loadingFolder.resolve("blank_records.csv");
        Path headerCsv = loadingFolder.resolve("headers_only.csv");
        Path emptyJson = loadingFolder.resolve("empty_records.json");

        Files.writeString(blankCsv, "");
        Files.writeString(headerCsv, "uri,date,title,body,url,source\n");
        Files.writeString(emptyJson, "[]");

        ArticleLoader loader = new ArticleLoader();

        assertTrue(loader.load(blankCsv.toString()).isEmpty());
        assertTrue(loader.load(headerCsv.toString()).isEmpty());
        assertTrue(loader.load(emptyJson.toString()).isEmpty());
    }

    //Reject a CSV that doesnt have the required columns
    @Test
    public void testMissingCsvColumns() throws IOException {

        Path incompleteFile = loadingFolder.resolve("incomplete_headers.csv");
        Files.writeString(incompleteFile, "uri,date,title\nrecord-63,2022-11-04,Depot update\n");

        ArticleLoader loader = new ArticleLoader();

        assertThrows(IOException.class, () -> loader.load(incompleteFile.toString()));
    }

    //Reject JSON with the wrong outer structure or broken syntax
    @Test
    public void testInvalidJsonFiles() throws IOException {

        Path objectFile = loadingFolder.resolve("single_object.json");
        Path brokenFile = loadingFolder.resolve("broken_records.json");

        Files.writeString(objectFile, "{\"uri\":\"record-82\"}");
        Files.writeString(brokenFile, "[{\"uri\":\"record-82\"");

        ArticleLoader loader = new ArticleLoader();

        assertThrows(IOException.class, () -> loader.load(objectFile.toString()));
        assertThrows(IOException.class, () -> loader.load(brokenFile.toString()));
    }

    //Missing supported files should report a loading error
    @Test
    public void testMissingArticleFiles() {

        ArticleLoader loader = new ArticleLoader();

        assertThrows(IOException.class, () -> loader.load(loadingFolder.resolve("absent_dispatch.csv").toString()));
        assertThrows(IOException.class, () -> loader.load(loadingFolder.resolve("absent_dispatch.json").toString()));
    }

    //Only CSV and JSON files are supported
    @Test
    public void testUnsupportedFileType() throws IOException {

        Path notesFile = loadingFolder.resolve("dispatch_notes.txt");
        Files.writeString(notesFile, "Dispatch notes");

        ArticleLoader loader = new ArticleLoader();

        assertThrows(IllegalArgumentException.class, () -> loader.load(notesFile.toString()));
    }
    
    //Check that skipped records leave warnings in the log
    @Test
    public void testSkippedRecordWarnings() throws IOException {

        Path inspectionFile = loadingFolder.resolve("inspection_records.csv");
        Path warningFile = loadingFolder.resolve("loading_warnings.log");

        String inspectionRows = "uri,date,title,body,url,source\n";
        inspectionRows += "inspection-26,2022-11-05,Scanner inspected,Readings stable,https://example.com/scanner,Inspection Notes\n";
        inspectionRows += "inspection-27,2022-02-30,Scanner checked,Readings recorded,https://example.com/check,Inspection Notes\n";
        inspectionRows += "inspection-26,2022-11-08,Repeated inspection,Readings repeated,https://example.com/repeat,Inspection Notes\n";

        Files.writeString(inspectionFile, inspectionRows);

        //Keep this tests warnings in its own temporary log
        AppLogger.init(warningFile.toString());

        try {
            ArticleLoader loader = new ArticleLoader();
            List<Article> inspectionArticles = loader.load(inspectionFile.toString());

            assertEquals(1, inspectionArticles.size());

        } finally {
            //Close the file even if an assertion fails
            AppLogger.getInstance().close();
        }

        String savedWarnings = Files.readString(warningFile);

        assertTrue(savedWarnings.contains("WARN"));
        assertTrue(savedWarnings.contains("Skipping CSV article inspection-27"));
        assertTrue(savedWarnings.contains("invalid date 2022-02-30"));
        assertTrue(savedWarnings.contains("Skipping duplicate article inspection-26"));
    }

}
