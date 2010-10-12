package org.grails.rabbitmq

import com.rabbitmq.client.AMQP.Queue.DeclareOk
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Consumer
import grails.test.GrailsUnitTestCase
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.FanoutExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory

class AutoQueueMessageListenerContainerTests extends GrailsUnitTestCase {
    def mockContext = new MockApplicationContext()
    def mockAdminBean = new Expando()
    def testContainer = new AutoQueueMessageListenerContainer()
    
    void setUp() {
        mockContext.registerMockBean("adm", mockAdminBean)
        
        testContainer.applicationContext = mockContext
        testContainer.connectionFactory = [
            createConnection: {-> [
                createChannel: {-> [
                    basicQos: {int qos -> },
                    queueDeclarePassive: {String s -> [:] as DeclareOk },
                    basicConsume: {String s, boolean b, Consumer c -> "Test" }
                ] as Channel }
            ] as Connection }
        ] as ConnectionFactory
    }
    
    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * topic exchange with the given name.
     */
    void testDoStartWithTopicExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.topic"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.queue == tempQueueName
            assert binding.routingKey == '#'
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new TopicExchange(exchangeName))
        
        testContainer.exchange = exchangeName
        testContainer.doStart()
        
        assertTrue "declareBinding() not called", declareBindingCalled
    }

    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * topic exchange with the given name and the given routing key.
     */
    void testDoStartWithTopicExchangeMap() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1235"
        def exchangeName = "another.topic"
        def routingKey = "my.routing.#"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.queue == tempQueueName
            assert binding.routingKey == routingKey
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new TopicExchange(exchangeName))
        
        testContainer.exchange = [ name: exchangeName, routingKey: routingKey ]
        testContainer.doStart()
        
        assertTrue "declareBinding() not called", declareBindingCalled
    }
    
    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * topic exchange with the given name and the 'match-all' wildcard routing
     * key when no routing key is given in the configuration map.
     */
    void testDoStartWithTopicExchangeMapNoRouting() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1235"
        def exchangeName = "another.topic"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.queue == tempQueueName
            assert binding.routingKey == '#'
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new TopicExchange(exchangeName))
        
        testContainer.exchange = [ name: exchangeName ]
        testContainer.doStart()
        
        assertTrue "declareBinding() not called", declareBindingCalled
    }
    
    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * fanout exchange with the given name. The routing key should not be set.
     */
    void testDoStartWithFanoutExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.fanout"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.queue == tempQueueName
            assert !binding.routingKey
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new FanoutExchange(exchangeName))
        
        testContainer.exchange = exchangeName
        testContainer.doStart()
        
        assertTrue "declareBinding() not called", declareBindingCalled
    }
    
    /**
     * Make sure that a temporary queue is created and that it is bound to the
     * fanout exchange with the given name. Even if a routing key is given, it
     * should be ignored.
     */
    void testDoStartWithFanoutExchangeMap() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1235"
        def exchangeName = "another.fanout"
        def routingKey = "my.routing.#"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            assert binding.exchange == exchangeName
            assert binding.queue == tempQueueName
            assert !binding.routingKey
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new FanoutExchange(exchangeName))
        
        testContainer.exchange = [ name: exchangeName, routingKey: routingKey ]
        testContainer.doStart()
        
        assertTrue "declareBinding() not called", declareBindingCalled
    }
    
    /**
     * No binding should be declared if the exchange is not a fanout or topic.
     */
    void testDoStartWithDirectExchangeName() {
        def declareBindingCalled = false
        def tempQueueName = "dummy-1234"
        def exchangeName = "my.direct"
        
        mockAdminBean.declareQueue = {-> return new Queue(tempQueueName) }
        mockAdminBean.declareBinding = { binding ->
            declareBindingCalled = true
        }
        
        mockContext.registerMockBean("grails.rabbit.exchange.${exchangeName}", new DirectExchange(exchangeName))
        
        testContainer.exchange = exchangeName
        testContainer.doStart()
        
        assertFalse "declareBinding() called", declareBindingCalled
    }
}
