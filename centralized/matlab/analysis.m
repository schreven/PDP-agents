close all
clc

plot(1:5000,probIgnore0)
hold on
plot(1:5000,probIgnore005)
plot(1:5000,probIgnore01b)


title("Dropout Analysis")
xlabel("Iterations")
ylabel("Objective Function")
legend(["dropout = 0" "dropout = 0.05" "dropout = 0.1"])

%%
clc
close all

plot(1:5000,addCurrent0)
hold on
plot(1:5000,addCurrent05)
plot(1:5000,addCurrent1)




