clc
clear all 
close all

%%

nTasks = [10 20 30 40 50 60];

final = [       1207 8677 -2289 13406;...
                3838 22648 856 22009;...
                5990 41144 2234 47529;...
                8749 52919 4333 62928;...
                9782 65138 5408 81542;...
                11781 79032 6586 99839];

aggressive = [  -2792 2062 -2576 8792;...
                -2063 16033 -1982 17395;...
                1032 34529 -305 42915;...
                1501 46304 1187 58314;...
                4013 58523 1815 76928;...
                5482 72417 2896 95225];

dummy = [       0 0 665 665;...
                798 798 1845 1845;...
                1849 1849 3291 3291;...
                4734 4734 5550 5550;...
                6173 6173 8524 8524;...
                15339 15339 14247 14247];
            
plot(nTasks, mean(final,2)); hold on
plot(nTasks, mean(aggressive,2)); 
plot(nTasks, mean(dummy,2)); 
legend('Final Agent', 'Aggressive Agent', 'Dummy Agent','Location','northwest');
xlabel('Number of Tasks');
ylabel('Average Final Profit');
title('Comparison between Agents');


%%
clc
close all
clear all

dd = [6500 7989 7989 3921 4231 4192];

fd = [8677 7154 7143 3994 3635 3684];

ff = [9121 6165 7583 4445 3752 4075];

p  = [9098 7056 7539 4486 3789 4146];

boxplot([ff' fd' dd' p'], 'labels', {'1','2','3','4'});
xlabel('Algorithm used')
ylabel('Final Profit')
title('Task distribution Algorithms Comparison')























