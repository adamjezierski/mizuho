package com.mizuho.test;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderBookTest {
	final OrderBook orderBook = new OrderBook();

	@Test
	public void testAddNull() {
		Exception thrown = assertThrows(NullPointerException.class, () -> orderBook.addOrder(null));
		assertEquals("newOrder is marked non-null but is null", thrown.getMessage());
	}

	@Test
	public void testAddOrder() throws Exception {
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);

		Set<Order> bidOrders = orderBook.getOrders('B').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		assertEquals(1, bidOrders.size());
		assertTrue(bidOrders.contains(order1));

		Set<Order> offerOrders = orderBook.getOrders('O').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		assertEquals(0, offerOrders.size());
		assertFalse(offerOrders.contains(order1));

		// Wrong side param
		Exception thrownWrongSide = assertThrows(Exception.class, () -> orderBook.addOrder(new Order(1L, 1.0, 'X', 10)));
		assertEquals("Wrong value for side, should be 'B' or 'O'", thrownWrongSide.getMessage());
	}

	@Test
	public void testAddOrderWithSameID() throws Exception {
		Order order = new Order(3L, 3.0, 'B', 30);
		orderBook.addOrder(order);

		Exception thrownIdAlreadyExists = assertThrows(Exception.class, () -> {
			Order sameIdOrder = new Order(3L, 3.0, 'B', 30);
			orderBook.addOrder(sameIdOrder);
		});

		assertEquals("Order with such ID already exists in database. Please update it instead", thrownIdAlreadyExists.getMessage());
	}

	@Test
	public void testRemoveOrder() throws Exception {
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);
		Order order2 = new Order(2L, 2.0, 'B', 20);
		orderBook.addOrder(order2);

		Order order3 = new Order(3L, 2.0, 'O', 10);
		orderBook.addOrder(order3);
		Order order4 = new Order(4L, 3.0, 'O', 20);
		orderBook.addOrder(order4);

		orderBook.removeOrder(1L);
		orderBook.removeOrder(4L);

		Set<Order> bidOrders = orderBook.getOrders('B').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		assertEquals(1, bidOrders.size());
		assertFalse(bidOrders.contains(order1));
		assertTrue(bidOrders.contains(order2));

		Set<Order> offerOrders = orderBook.getOrders('O').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		assertEquals(1, offerOrders.size());
		assertTrue(offerOrders.contains(order3));
		assertFalse(offerOrders.contains(order4));
	}

	@Test
	public void testUpdateOrder() throws Exception {
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);

		Set<Order> bidOrders = orderBook.getOrders('B').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		Optional<Order> findOrderByIdOptional = bidOrders.stream().filter(order -> order.getId() == 1L).findFirst();
		assertTrue(findOrderByIdOptional.isPresent());
		assertEquals(10, findOrderByIdOptional.get().getSize());

		orderBook.updateOrder(1L, 30);

		Set<Order> bidOrdersAfterUpdate = orderBook.getOrders('B').values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
		Optional<Order> findOrderByIdAfterUpdateOptional = bidOrdersAfterUpdate.stream().filter(order -> order.getId() == 1L).findFirst();
		assertTrue(findOrderByIdAfterUpdateOptional.isPresent());
		assertEquals(30, findOrderByIdAfterUpdateOptional.get().getSize());
	}

	@Test
	public void testGetPriceForLevel() throws Exception {
		Order order5 = new Order(5L, 3.0, 'B', 50);
		orderBook.addOrder(order5);
		Order order3 = new Order(3L, 3.0, 'B', 30);
		orderBook.addOrder(order3);
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);
		Order order2 = new Order(2L, 2.0, 'B', 20);
		orderBook.addOrder(order2);

		Order order4 = new Order(4L, 2.0, 'O', 10);
		orderBook.addOrder(order4);
		Order order6 = new Order(6L, 3.0, 'O', 20);
		orderBook.addOrder(order6);

		Exception thrownNegativeLevel = assertThrows(Exception.class, () -> orderBook.getPriceForLevel('B', -1));
		assertEquals("Level needs to be >0", thrownNegativeLevel.getMessage());

		Exception thrownWrongSide = assertThrows(Exception.class, () -> orderBook.getPriceForLevel('X', 1));
		assertEquals("Wrong value for side, should be 'B' or 'O'", thrownWrongSide.getMessage());

		assertTrue(orderBook.getPriceForLevel('B', 1).isPresent());
		assertEquals(3.0, orderBook.getPriceForLevel('B', 1).get());

		assertTrue(orderBook.getPriceForLevel('B', 2).isPresent());
		assertEquals(2.0, orderBook.getPriceForLevel('B', 2).get());

		assertTrue(orderBook.getPriceForLevel('B', 3).isPresent());
		assertEquals(1.0, orderBook.getPriceForLevel('B', 3).get());

		assertFalse(orderBook.getPriceForLevel('B', 60).isPresent());

		assertTrue(orderBook.getPriceForLevel('O', 1).isPresent());
		assertEquals(2.0, orderBook.getPriceForLevel('O', 1).get());

		assertTrue(orderBook.getPriceForLevel('O', 2).isPresent());
		assertEquals(3.0, orderBook.getPriceForLevel('O', 2).get());

		assertFalse(orderBook.getPriceForLevel('O', 60).isPresent());
	}

	@Test
	public void testGetTotalOrderSize() throws Exception {
		Order order5 = new Order(5L, 3.0, 'B', 50);
		orderBook.addOrder(order5);
		Order order3 = new Order(3L, 3.0, 'B', 30);
		orderBook.addOrder(order3);
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);
		Order order2 = new Order(2L, 2.0, 'B', 20);
		orderBook.addOrder(order2);

		Exception thrownWrongSide = assertThrows(Exception.class, () -> orderBook.getTotalOrderSize('X', 1));
		assertEquals("Wrong value for side, should be 'B' or 'O'", thrownWrongSide.getMessage());

		assertTrue(orderBook.getTotalOrderSize('B', 1).isPresent());
		assertEquals(80, orderBook.getTotalOrderSize('B', 1).get());

		assertTrue(orderBook.getTotalOrderSize('B', 2).isPresent());
		assertEquals(20, orderBook.getTotalOrderSize('B', 2).get());

		assertTrue(orderBook.getTotalOrderSize('B', 3).isPresent());
		assertEquals(10, orderBook.getTotalOrderSize('B', 3).get());

		Exception thrown = assertThrows(Exception.class, () -> orderBook.getTotalOrderSize('B', -1));
		assertEquals("Level needs to be >0", thrown.getMessage());

		assertFalse(orderBook.getTotalOrderSize('B', 60).isPresent());
	}

	@Test
	public void testDisplayOrderMap() throws Exception {
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);
		Order order2 = new Order(2L, 2.0, 'B', 20);
		orderBook.addOrder(order2);

		Exception thrownWrongSide = assertThrows(Exception.class, () -> orderBook.displayOrderMap('X'));
		assertEquals("Wrong value for side, should be 'B' or 'O'", thrownWrongSide.getMessage());

		String expectedResult =
				"{\n" + "  \"1.0\": [\n" + "    {\n" + "      \"id\": 1,\n" + "      \"price\": 1.0,\n" + "      \"side\": \"B\",\n" + "      \"size\": 10\n"
						+ "    }\n" + "  ],\n" + "  \"2.0\": [\n" + "    {\n" + "      \"id\": 2,\n" + "      \"price\": 2.0,\n" + "      \"side\": \"B\",\n"
						+ "      \"size\": 20\n" + "    }\n" + "  ]\n" + "}";

		assertEquals(expectedResult, orderBook.displayOrderMap('B'));
	}

	@Test
	public void testGetOrders() throws Exception {
		Order order5 = new Order(5L, 3.0, 'B', 50);
		orderBook.addOrder(order5);
		Order order3 = new Order(3L, 3.0, 'B', 30);
		orderBook.addOrder(order3);
		Order order1 = new Order(1L, 1.0, 'B', 10);
		orderBook.addOrder(order1);
		Order order2 = new Order(2L, 2.0, 'B', 20);
		orderBook.addOrder(order2);

		Order order4 = new Order(4L, 2.0, 'O', 10);
		orderBook.addOrder(order4);
		Order order6 = new Order(6L, 3.0, 'O', 20);
		orderBook.addOrder(order6);

		// Check level order
		Iterator<Double> bidPricesIterator = orderBook.getOrders('B').keySet().iterator();
		assertEquals(3, bidPricesIterator.next());
		assertEquals(2, bidPricesIterator.next());
		assertEquals(1, bidPricesIterator.next());

		// Check time-order
		LinkedHashSet<Order> bidOrdersForPrice = orderBook.getOrders('B').get(3.0);
		assertEquals(2, bidOrdersForPrice.size());
		Iterator<Order> orderIterator = bidOrdersForPrice.iterator();
		assertEquals(5L, orderIterator.next().getId());
		assertEquals(3L, orderIterator.next().getId());

		// Check level order
		Iterator<Double> offersPricesIterator = orderBook.getOrders('O').keySet().iterator();
		assertEquals(2, offersPricesIterator.next());
		assertEquals(3, offersPricesIterator.next());

		// Wrong side param
		Exception thrownWrongSide = assertThrows(Exception.class, () -> orderBook.getOrders('X'));
		assertEquals("Wrong value for side, should be 'B' or 'O'", thrownWrongSide.getMessage());
	}
}
