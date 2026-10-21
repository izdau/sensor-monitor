package com.somecompany.listener;

import com.somecompany.enums.SensorType;
import com.somecompany.producer.SensorMeasurementProducer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"conf.sensor.temperature-port=33344", "conf.sensor.humidity-port=33355"}) // not the real ports, in case the app is running
class UdpSensorDataListenerTests {

    @MockitoBean
    private SensorMeasurementProducer sensorMeasurementProducer;

    @Test
    void validSensorDataShouldBeSentAndInvalidSensorDataShouldBeDiscarded() throws IOException {
        when(sensorMeasurementProducer.send(any())).thenReturn(Mono.empty());

        send(33344, "garbage"); // dropped, and handled before the next data of the same port
        send(33344, "sensor_id=t1; value=36.5\n");
        send(33355, "sensor_id=h1; value=40");

        verify(sensorMeasurementProducer, timeout(3000)).send(argThat(event ->
                event.sensorType() == SensorType.TEMPERATURE && event.sensorId().equals("t1") && event.value() == 36.5));
        verify(sensorMeasurementProducer, timeout(3000)).send(argThat(event ->
                event.sensorType() == SensorType.HUMIDITY && event.sensorId().equals("h1") && event.value() == 40));
        verify(sensorMeasurementProducer, times(2)).send(any());
    }

    private static void send(int port, String sensorData) throws IOException {
        byte[] bytes = sensorData.getBytes(StandardCharsets.US_ASCII);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.send(new DatagramPacket(bytes, bytes.length, InetAddress.getLoopbackAddress(), port));
        }
    }
}
