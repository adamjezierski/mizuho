package com.mizuho.test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {

	private final Map<Character, Map<Double, LinkedHashSet<Order>>> orders = new HashMap<>() {{
		put('B', new TreeMap<>());
		put('O', new TreeMap<>());
	}};

	public void addOrder(@NonNull Order newOrder) throws Exception {
		validateSideValue(newOrder.getSide());
		if (checkIfOrderWithSuchIdExists(newOrder)) {
			throw new Exception("Order with such ID already exists in database. Please update it instead");
		} else {
			addOrderByType(newOrder, orders.get(newOrder.getSide()));
		}
	}

	private boolean checkIfOrderWithSuchIdExists(final Order newOrder) {
		return orders.values()
				.stream()
				.flatMap(mapOfPricesWithRelatedOrders -> mapOfPricesWithRelatedOrders.values().stream().flatMap(Collection::stream))
				.anyMatch(order -> order.getId() == newOrder.getId());
	}

	private void addOrderByType(Order newOrder, Map<Double, LinkedHashSet<Order>> orderMap) {
		LinkedHashSet<Order> orderList = orderMap.get(newOrder.getPrice());
		if (orderList == null) {
			orderList = new LinkedHashSet<>();
		}
		orderList.add(newOrder);
		orderMap.put(newOrder.getPrice(), orderList);
	}

	public void removeOrder(long orderIdToRemove) {
		orders.values()
				.forEach(mapOfPricesWithRelatedOrders -> mapOfPricesWithRelatedOrders.values()
						.forEach(orders -> orders.removeIf(order -> order.getId() == orderIdToRemove)));
	}

	public void updateOrder(long orderIdToUpdate, long newSize) {
		orders.values()
				.stream()
				.flatMap(mapOfPricesWithRelatedOrders -> mapOfPricesWithRelatedOrders.values().stream().flatMap(Collection::stream))
				.filter(order -> order.getId() == orderIdToUpdate)
				.forEach(orderToUpdate -> {
					try {
						Field orderSizeField = orderToUpdate.getClass().getDeclaredField("size");
						orderSizeField.setAccessible(true);
						orderSizeField.setLong(orderToUpdate, newSize);
					} catch (IllegalAccessException | NoSuchFieldException e) {
						throw new RuntimeException("Unable to update size value of Order", e);
					}
				});
	}

	public Optional<Double> getPriceForLevel(char side, int level) throws Exception {
		validateSideValue(side);
		validateIfLevelIsPositive(level);
		List<Double> keysSortedDesc = orders.get(side).keySet().stream().sorted(getSortComparator(side)).collect(Collectors.toList());
		if (keysSortedDesc.size() >= level) {
			return Optional.of(keysSortedDesc.get(level - 1));
		}
		return Optional.empty();
	}

	public Optional<Long> getTotalOrderSize(char side, int level) throws Exception {
		validateSideValue(side);
		validateIfLevelIsPositive(level);
		return getPriceForLevel(side, level).map(priceLevel -> orders.get(side).get(priceLevel).stream().mapToLong(Order::getSize).sum());
	}

	private void validateIfLevelIsPositive(int level) throws Exception {
		if (level <= 0) {
			throw new Exception("Level needs to be >0");
		}
	}

	public String displayOrderMap(char side) throws Exception {
		validateSideValue(side);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(orders.get(side));
	}

	public Map<Double, LinkedHashSet<Order>> getOrders(char side) throws Exception {
		validateSideValue(side);
		return orders.get(side)
				.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey(getSortComparator(side)))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldValue, newValue) -> oldValue, LinkedHashMap::new));
	}

	private void validateSideValue(char side) throws Exception {
		if (!List.of('B', 'O').contains(side)) {
			throw new Exception("Wrong value for side, should be 'B' or 'O'");
		}
	}

	private Comparator<Double> getSortComparator(char side) {
		if (side == 'B') {
			return Comparator.reverseOrder();
		} else {
			return Comparator.naturalOrder();
		}
	}
}
