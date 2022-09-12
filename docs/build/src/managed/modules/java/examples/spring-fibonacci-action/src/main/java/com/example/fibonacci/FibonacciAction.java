package com.example.fibonacci;

import kalix.javasdk.action.Action;

import java.util.function.Predicate;

import org.springframework.web.bind.annotation.*;

@RequestMapping("/fibonacci")
public class FibonacciAction extends Action { 

  private boolean isFibonacci(long num) {  // <1>
    Predicate<Long> isPerfectSquare = (n) -> {
      long square = (long) Math.sqrt(n);
      return square*square == n;
    };
    return isPerfectSquare.test(5*num*num + 4) || isPerfectSquare.test(5*num*num - 4);
  }
  private long nextFib(long num) {
    double result = num * (1 + Math.sqrt(5)) / 2.0;
    return Math.round(result);
  }

  @GetMapping("/{number}/next")
  public Effect<Number> nextNumber(@PathVariable Long number) {
    return nextNumber(new Number(number));
  }

  @PostMapping("/next")
  public Effect<Number> nextNumber(@RequestBody Number number) {
    long num =  number.value;
    if (isFibonacci(num)) {
      return effects().reply(new Number(nextFib(num)));
    } else {
      return effects()
          .error("Input number is not a Fibonacci number, received '" + num + "'");
    }
  }
}