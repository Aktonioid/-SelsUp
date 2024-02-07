package selsup.app;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CrptApi
{
    private final static AtomicInteger COUNTER = new AtomicInteger(0);
    private Integer maxCount;
    private long lastResetTime; //Время последнего сброса счетчика
    private long timeLimit; // лимит времени для запросов

    public CrptApi(int maxCount,
                TimeUnit timeUnit, //Указать промежуток времени
                long duration // численно указать промежуток (TimeUnit - seconds, duration - ограничение по времени в секунах)
                ) 
    {   
        //можно сделать приведение любых запросов/единицу времени к запросов/секунду
        // но я не знаю нужно ли это по заданию, так что не реализовал        
        this.maxCount = maxCount;
        lastResetTime = System.currentTimeMillis();
        this.timeLimit = timeUnit.toMillis(duration);
        
    }    

    // По заданию метод должен принимать документ и подпись, если я правмльно понял, это токен авторизации
    public void CreateRequest(Document document, String token) throws InterruptedException
    {
        
        //проверка на то надо ли сбросить колличество запросов
        CheckRequestsReset();
        System.out.println(" 1");

        // проверка на то не привышает ли колличество запросов за единицу времени 
        if(COUNTER.get() >= maxCount)
        {
            System.out.println("Greater");
            // по факту просто морозим потоки, которые пытаются отправить запрос
            // не самая 
            while (true) 
            {
                Thread.sleep(100);

                CheckRequestsReset();

                // Если еще есть место для отправки прекращаем цикл
                if (COUNTER.get() < maxCount) 
                {
                    break;
                }
            }
        }

        // увеличиваем на 1 колличество запросов
        COUNTER.incrementAndGet();
        
        // SendPostRequest(
        //     CreateJson(document), 
        //     token, 
        //     "https://ismp.crpt.ru/api/v3/lk/documents/create");
        System.out.println(SendPostrequest(CreateJson(document), token));
    }

    // проверка н ато необходимо ли сбрасывать колличество запросов
    // при необходимости сбрасывает
    private boolean CheckRequestsReset()
    {
        if(System.currentTimeMillis() - lastResetTime >= timeLimit)
        {
            lastResetTime = System.currentTimeMillis();
            COUNTER.set(0);
            System.out.print("reset");
            return true;
        }

        return false;
    }

    // сериализация в json
    private String CreateJson(Document document)
    {
        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        
        try 
        {
            mapper.writeValue(writer, document);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
            return null;
        }

        return writer.toString();
    }

    // метод для отправки post запросов 
    private String SendPostRequest(String json, String token, String url)
    {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest
            .newBuilder(URI.create(url))
            .POST(BodyPublishers.ofString(json))
            .header("X-Auth-Token", token)
            .build();
        String response = null;
            try 
            {
                response = client.send(request, BodyHandlers.ofString()).body();
            } 
            catch (IOException | InterruptedException e) 
            {
                e.printStackTrace();
                response = "error";
            }
        return response;
    }

    // Метод, который просто поток уводит в sleep имитируя отправку сообщения, чтоб не отаравлять запрос по ссылке из задания
    private String SendPostrequest(String json, String token)
    {
        System.out.println("Sending request with body :");
        System.out.println(json);
        System.out.println("\n AuthToken is" + token);
        try 
        {
            Thread.currentThread().sleep(5000); // приостановка потока на секунду, тип симуляция отправки запроса
        } 
        catch (InterruptedException e) 
        {
        }//
        return "Sended";
    }


    @JsonAutoDetect
    public class Document 
    {
        public String doc_id;

        public String doc_status;
        
        public String doc_type;
        
        public boolean importRequest;
        
        public String owner_inn;
        
        public String participant_inn;
        
        public String producer_inn;
        
        public String production_date;
        
        public String production_type;
        
        public Description description;
        
        public Product[] products;

        public Document(
            String doc_id, 
            String doc_status, 
            String doc_type, 
            String participantInn,
            boolean importRequest,
            String owner_inn,
            String producer_inn,
            Date production_date,
            String production_type,
            Product[] products
            )
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.description = new Description(participantInn);
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participantInn;
            this.producer_inn = producer_inn;
            this.production_date = df.format(production_date);
            this.production_type = production_type;
            this.products = products;
        }
        
    }
    public class Description 
    {
        public String participantInn;

        public Description(String participantInn)
        {
            this.participantInn = participantInn;
        }
    }

    public class Product 
    {
        public Product
        (
            String certificate_document,
            Date certificate_document_date,
            String certificate_document_number,
            String owner_inn,
            String producer_inn,
            Date production_date,
            String tnved_code,
            String uit_code,
            String uitu_code
        )
        {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");

            this.certificate_document = certificate_document;
            this.certificate_document_date = df.format(production_date);
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = df.format(production_date);
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;
        
    }
}
